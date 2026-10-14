package dev.vadim.bcenchantments.weapon;

import dev.vadim.bcenchantments.Particles;
import dev.vadim.bcenchantments.TrimEnchantments;
import dev.vadim.bcenchantments.combat.ComboTracker;
import java.util.Map;
import java.util.WeakHashMap;
import net.bettercombat.api.AttackHand;
import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.logic.PlayerAttackHelper;
import net.bettercombat.logic.WeaponRegistry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;

/**
 * The trident: the reach and mobility weapon, defined in
 * {@code data/bcenchantments/weapon_attributes/trident.json}. Two quick jabs and a long lunge, with
 * a little less reach than the spear. Its mechanic is Perfect Spacing: a thrust that meets its target
 * near the end of its reach deals {@link #PERFECT} times the damage. Up close it is only a short
 * stick, so the sword keeps close combat and the spear keeps versatile reach. The jabs push the
 * target back to the sweet spot; the lunge pushes it further.
 *
 * <p>Its vanilla enchantments feed the mechanic. Loyalty widens the sweet spot inward. Riptide makes
 * every thrust during its spin, and shortly after it, a Perfect Spacing hit, so the player dives in and
 * strikes. Channeling calls lightning onto a target struck by a Perfect Spacing lunge in a thunderstorm,
 * where the sky is open above it, like a thrown trident does.
 */
public final class TridentStyle implements WeaponStyle {
	public static final TridentStyle INSTANCE = new TridentStyle();

	public static final String THRUST_1 = TrimEnchantments.MOD_ID + ":trident_thrust_1";
	public static final String THRUST_2 = TrimEnchantments.MOD_ID + ":trident_thrust_2";
	public static final String LUNGE = TrimEnchantments.MOD_ID + ":trident_lunge";

	/** Attack cooldown of each thrust, relative to the weapon's own. */
	private static final Map<String, Float> COOLDOWN = Map.of(
		THRUST_1, 0.75F,
		THRUST_2, 0.75F,
		LUNGE, 1.35F
	);

	/** Knockback of the lunge, relative to a normal hit's; the jabs keep a normal hit's. */
	private static final double LUNGE_KNOCKBACK = 1.6;

	/** Damage of a Perfect Spacing hit. */
	public static final float PERFECT = 1.3F;
	/** The sweet spot starts at this share of the thrust's reach and runs to its end. */
	public static final double SWEET_SPOT = 0.75;
	/** Each Loyalty level starts the sweet spot this much nearer. */
	public static final double SWEET_SPOT_PER_LOYALTY = 0.05;
	/** Thrusts this long after a Riptide spin ends still count as Perfect Spacing: 1.5 seconds. */
	public static final int RIPTIDE_TICKS = 30;

	/** The tick each player last spun with Riptide. */
	private static final Map<ServerPlayer, Integer> LAST_SPIN = new WeakHashMap<>();

	private TridentStyle() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if (player.isAutoSpinAttack()) {
					LAST_SPIN.put(player, player.tickCount);
				}
			}
		});
	}

	public static boolean isTridentAttack(String animation) {
		return animation != null && COOLDOWN.containsKey(animation);
	}

	/** A trident is any weapon whose combo opens with the trident's first jab. */
	@Override
	public boolean matches(ItemStack stack) {
		WeaponAttributes attributes = WeaponRegistry.getAttributes(stack);
		return attributes != null
			&& attributes.attacks() != null
			&& attributes.attacks().length > 0
			&& THRUST_1.equals(attributes.attacks()[0].animation());
	}

	@Override
	public boolean shapesCooldown() {
		return true;
	}

	@Override
	public float cooldownFactor(String animation) {
		return COOLDOWN.getOrDefault(animation, 1.0F);
	}

	@Override
	public double knockbackFactor(String animation) {
		return LUNGE.equals(animation) ? LUNGE_KNOCKBACK : 1.0;
	}

	/**
	 * The damage of {@code player}'s thrust on {@code target}: raised for Perfect Spacing, with a
	 * splash on the target, and Channeling's lightning on a perfect lunge.
	 */
	public static float damage(Player player, Entity target, float amount) {
		if (!(player instanceof ServerPlayer server)) {
			return amount;
		}
		AttackHand swing = ComboTracker.currentSwing(server);
		if (swing == null || !isTridentAttack(swing.attack().animation())) {
			return amount;
		}
		ItemStack weapon = swing.itemStack();
		ServerLevel world = server.level();
		boolean riptide = LAST_SPIN.containsKey(server) && server.tickCount - LAST_SPIN.get(server) <= RIPTIDE_TICKS;
		if (!riptide && !spaced(server, weapon, target, enchantment(world, weapon, Enchantments.LOYALTY))) {
			return amount;
		}
		Particles.send(world, ParticleTypes.SPLASH, target.getX(), target.getY(0.6), target.getZ(), 30, 0.3, 0.35, 0.3, 0.2);
		world.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 0.8F, 1.3F);
		if (LUNGE.equals(swing.attack().animation()) && enchantment(world, weapon, Enchantments.CHANNELING) > 0) {
			channel(server, world, target);
		}
		return amount * PERFECT;
	}

	/** Whether {@code target}'s nearest point sits in the sweet spot at the end of the thrust's reach. */
	private static boolean spaced(ServerPlayer player, ItemStack weapon, Entity target, int loyalty) {
		double reach = PlayerAttackHelper.getRangeForItem(player, weapon);
		Vec3 eye = player.getEyePosition();
		Vec3 nearest = new Vec3(
			Math.clamp(eye.x, target.getBoundingBox().minX, target.getBoundingBox().maxX),
			Math.clamp(eye.y, target.getBoundingBox().minY, target.getBoundingBox().maxY),
			Math.clamp(eye.z, target.getBoundingBox().minZ, target.getBoundingBox().maxZ));
		return eye.distanceTo(nearest) >= reach * Math.max(0.0, SWEET_SPOT - SWEET_SPOT_PER_LOYALTY * loyalty);
	}

	private static void channel(ServerPlayer player, ServerLevel world, Entity target) {
		if (target instanceof ArmorStand || !world.isThundering() || !world.canSeeSky(target.blockPosition())) {
			return;
		}
		LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(world, EntitySpawnReason.TRIGGERED);
		if (bolt != null) {
			bolt.snapTo(target.position());
			bolt.setCause(player);
			world.addFreshEntity(bolt);
		}
	}

	private static int enchantment(ServerLevel world, ItemStack weapon, ResourceKey<Enchantment> key) {
		Holder<Enchantment> holder = world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
		return EnchantmentHelper.getItemEnchantmentLevel(holder, weapon);
	}
}
