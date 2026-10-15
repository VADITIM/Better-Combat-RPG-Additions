package dev.vadim.bcenchantments.mob;

import com.mojang.serialization.Codec;
import dev.vadim.bcenchantments.Particles;
import dev.vadim.bcenchantments.TrimEnchantments;
import dev.vadim.bcenchantments.progression.Refinement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.jspecify.annotations.Nullable;

/**
 * How strong a player's weapons make the monsters around them, and which trim power those monsters
 * learn to counter.
 * <p>
 * Every refined weapon the player carries counts on its own: its power levels plus a bonus per
 * power pair. The strongest counts in full and each further one less ({@link MobBalance#WEAPON_FALLOFF}),
 * so a second weapon raises the difficulty, a fifth barely does.
 * <p>
 * Each weapon also remembers how often each of its powers fired ({@link Usage}, saved on the item).
 * The power furthest toward full adaptation, across the weapons carried, is the one monsters adapt
 * to: up to {@link MobBalance#COUNTER_MAX_SHARE} of new monsters near the player spawn countering it.
 * A countering monster shrugs off that power's control (pull, push, launch, root, chill) and takes
 * a quarter of its damage. Leaning on another weapon, or leaving the old one behind, moves them on;
 * a weapon that is lost or destroyed takes its count with it. Both sides compute the same answer
 * from the inventory, so the inventory panel shows what the server spawns with.
 */
public final class Adaptation {
	/** How many times each power on a weapon fired, by power id ({@code lightning}), capped at full adaptation. */
	public record Usage(Map<String, Integer> uses) {
		public static final Usage NONE = new Usage(Map.of());
		public static final Codec<Usage> CODEC = Codec.unboundedMap(Codec.STRING, Codec.INT).xmap(Usage::new, Usage::uses);

		public Usage {
			uses = Map.copyOf(uses);
		}

		public int of(Refinement.Perk perk) {
			return uses.getOrDefault(perk.id(), 0);
		}
	}

	public static final DataComponentType<Usage> USAGE = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
		TrimEnchantments.id("power_usage"),
		DataComponentType.<Usage>builder().persistent(Usage.CODEC)
			.networkSynchronized(ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT).map(Usage::new, u -> new HashMap<>(u.uses())))
			.build());

	/** A weapon the player carries and what it adds: {@code power} times {@code weight}. */
	public record Weapon(ItemStack stack, int power, double weight) {
		public double counted() {
			return power * weight;
		}
	}

	/**
	 * A player's standing: their weapons, strongest first; the power they add up to, capped at
	 * {@link #maxPower}; the power monsters adapt to (null if none yet) and how far along it is, 0 to 1.
	 */
	public record Profile(List<Weapon> weapons, double power, Refinement.@Nullable Perk adapted, double progress) {
		public static final Profile NONE = new Profile(List.of(), 0.0, null, 0.0);

		/** The share of new monsters nearby that counter {@link #adapted}. */
		public double counterShare() {
			return adapted == null ? 0.0 : MobBalance.COUNTER_MAX_SHARE * progress;
		}
	}

	/** When each countering monster last showed that it resisted, so a pull every tick does not clang every tick. */
	private static final Map<LivingEntity, Integer> SHOWN = new WeakHashMap<>();
	private static final int FEEDBACK_TICKS = 10;

	private Adaptation() {
	}

	static void init() {
		// Loading the class registers the component.
	}

	public static int maxPower() {
		return MobBalance.MAX_POWER;
	}

	public static double maxCounterShare() {
		return MobBalance.COUNTER_MAX_SHARE;
	}

	/** Fires of {@code perk} to full adaptation: frequent powers need many, charged ones few. */
	public static int full(Refinement.Perk perk) {
		return switch (perk) {
			case COLD_EDGE, GRAVITY -> MobBalance.ADAPT_EVERY_SWING;
			case LIGHTNING, SHOCKWAVE, CHAINS, ECHO, WITHERING, SHULKER_BURST -> MobBalance.ADAPT_CHARGED;
			default -> MobBalance.ADAPT_CONDITIONAL;
		};
	}

	/** One weapon's own power: its trim power levels plus a bonus for each pair. */
	public static int weaponPower(ItemStack stack) {
		if (Refinement.isShield(stack)) {
			return 0;
		}
		List<Refinement.Power> powers = Refinement.powersOf(stack);
		int power = MobBalance.PAIR_POWER * (powers.size() / 2);
		for (Refinement.Power each : powers) {
			power += each.level();
		}
		return power;
	}

	public static Profile of(Player player) {
		Inventory inventory = player.getInventory();
		List<ItemStack> carried = new ArrayList<>();
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (!stack.isEmpty() && weaponPower(stack) > 0) {
				carried.add(stack);
			}
		}
		if (carried.isEmpty()) {
			return Profile.NONE;
		}
		carried.sort(Comparator.comparingInt(Adaptation::weaponPower).reversed());
		List<Weapon> weapons = new ArrayList<>(carried.size());
		double power = 0.0;
		double weight = 1.0;
		Map<Refinement.Perk, Integer> uses = new HashMap<>();
		for (ItemStack stack : carried) {
			Weapon weapon = new Weapon(stack, weaponPower(stack), weight);
			weapons.add(weapon);
			power += weapon.counted();
			weight *= MobBalance.WEAPON_FALLOFF;
			Usage usage = stack.getOrDefault(USAGE, Usage.NONE);
			for (Refinement.Power each : Refinement.powersOf(stack)) {
				Refinement.Perk perk = each.perk();
				if (perk != null && perk.built()) {
					uses.merge(perk, usage.of(perk), Integer::sum);
				}
			}
		}
		Refinement.Perk adapted = null;
		double progress = 0.0;
		for (Map.Entry<Refinement.Perk, Integer> entry : uses.entrySet()) {
			double p = Math.min(1.0, entry.getValue() / (double) full(entry.getKey()));
			if (p > progress) {
				progress = p;
				adapted = entry.getKey();
			}
		}
		return new Profile(List.copyOf(weapons), Math.min(power, MobBalance.MAX_POWER), adapted, progress);
	}

	/**
	 * Counts one fire of {@code key} by {@code weapon}, up to full adaptation. Written to the stack in
	 * the player's hand, in case {@code weapon} is a copy of it.
	 */
	public static void used(Player player, ItemStack held, ResourceKey<Enchantment> key) {
		Refinement.Perk perk = Refinement.Perk.of(key);
		ItemStack weapon = live(player, held);
		if (perk == null || weapon.isEmpty() || Refinement.perkLevel(weapon, key) <= 0) {
			return;
		}
		Usage usage = weapon.getOrDefault(USAGE, Usage.NONE);
		int now = usage.of(perk);
		if (now >= full(perk)) {
			return;
		}
		Map<String, Integer> uses = new HashMap<>(usage.uses());
		uses.put(perk.id(), now + 1);
		weapon.set(USAGE, new Usage(uses));
	}

	private static ItemStack live(Player player, ItemStack weapon) {
		for (ItemStack hand : List.of(player.getMainHandItem(), player.getOffhandItem())) {
			if (hand == weapon || ItemStack.matches(hand, weapon)) {
				return hand;
			}
		}
		return ItemStack.EMPTY;
	}

	// ---- Countering monsters ----

	private static String tag(Refinement.Perk perk) {
		return TrimEnchantments.MOD_ID + ".counter." + perk.id();
	}

	/** The power {@code entity} counters, or null. */
	public static Refinement.@Nullable Perk counter(Entity entity) {
		for (String tag : entity.entityTags()) {
			if (tag.startsWith(TrimEnchantments.MOD_ID + ".counter.")) {
				for (Refinement.Perk perk : Refinement.Perk.values()) {
					if (tag.equals(tag(perk))) {
						return perk;
					}
				}
			}
		}
		return null;
	}

	/**
	 * Whether {@code target} counters {@code key}; if so it shows it, a puff and a dull clang (at most
	 * twice a second, for powers that pull every tick), so the player sees why the power did little.
	 */
	public static boolean resists(LivingEntity target, ResourceKey<Enchantment> key) {
		Refinement.Perk perk = counter(target);
		if (perk == null || !perk.enchantment().equals(key)) {
			return false;
		}
		Integer shown = SHOWN.get(target);
		if (target.level() instanceof ServerLevel world && (shown == null || target.tickCount - shown >= FEEDBACK_TICKS || target.tickCount < shown)) {
			SHOWN.put(target, target.tickCount);
			Particles.send(world, ParticleTypes.ENCHANTED_HIT, target.getX(), target.getY(0.6), target.getZ(), 6, 0.3, 0.3, 0.3, 0.05);
			world.playSound((Entity) null, target.getX(), target.getY(), target.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 0.4F, 1.4F);
		}
		return true;
	}

	/** What is left of a countered power's {@code damage}: a quarter. */
	public static float reduced(float damage) {
		return damage * MobBalance.COUNTER_DAMAGE;
	}

	/** {@code damage} from the power {@code key} as it lands on {@code target}, {@link #reduced} if it counters it. */
	public static float damage(LivingEntity target, ResourceKey<Enchantment> key, float damage) {
		return resists(target, key) ? reduced(damage) : damage;
	}

	/**
	 * Maybe makes a fresh monster counter the power its strongest-adapted nearby player leans on, with
	 * that player's {@link Profile#counterShare} as the chance.
	 */
	static void maybeCounter(ServerLevel world, Mob mob) {
		if (mob instanceof WitherBoss || mob instanceof EnderDragon) {
			return;
		}
		Profile best = Profile.NONE;
		double radiusSqr = MobBalance.POWER_RADIUS * MobBalance.POWER_RADIUS;
		for (ServerPlayer player : world.players()) {
			if (!player.isSpectator() && player.distanceToSqr(mob) <= radiusSqr) {
				Profile profile = of(player);
				if (profile.counterShare() > best.counterShare()) {
					best = profile;
				}
			}
		}
		Refinement.Perk perk = best.adapted();
		if (perk == null || mob.getRandom().nextDouble() >= best.counterShare()) {
			return;
		}
		mob.addTag(tag(perk));
		Component name = mob.hasCustomName() ? mob.getCustomName() : mob.getType().getDescription();
		mob.setCustomName(Component.translatable("mob.bcenchantments.counter." + perk.id(), name));
	}

	/** The profile of the player near {@code mob} whose weapons add the most power. */
	static Profile strongest(ServerLevel world, Mob mob) {
		Profile best = Profile.NONE;
		double radiusSqr = MobBalance.POWER_RADIUS * MobBalance.POWER_RADIUS;
		for (ServerPlayer player : world.players()) {
			if (!player.isSpectator() && player.distanceToSqr(mob) <= radiusSqr) {
				Profile profile = of(player);
				if (profile.power() > best.power()) {
					best = profile;
				}
			}
		}
		return best;
	}
}
