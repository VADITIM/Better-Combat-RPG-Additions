package dev.vadim.bcenchantments.perk;

import dev.vadim.bcenchantments.Particles;
import dev.vadim.bcenchantments.TrimEnchantments;
import dev.vadim.bcenchantments.network.AirMovePayload;
import dev.vadim.bcenchantments.weapon.MaceStyle;
import dev.vadim.bcenchantments.weapon.SpearStyle;
import dev.vadim.bcenchantments.weapon.SwordStyle;
import dev.vadim.bcenchantments.weapon.WeaponStyle;
import dev.vadim.bcenchantments.weapon.WeaponStyles;
import dev.vadim.bcenchantments.network.UpdraftLaunchPayload;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * {@link Synergy#UPDRAFT}'s air move: right click in the air with an Updraft weapon, once per jump or
 * launch, does something different for each weapon kind. Left click stays a normal attack outside it.
 * <ul>
 * <li>Mace: slams straight down fast and smashes the ground for 1.5 times a mace smash from that height,
 * only right around the player (1.5 blocks across).</li>
 * <li>Sword: dives straight down, a quarter slower than the slam, and cuts everything around the landing spot.</li>
 * <li>Spear: only after an Updraft launch, and only without the Lunge enchantment: lunges the way the
 * player looks, striking every enemy it runs through once with the spear's damage. Lunge itself works in
 * the air only after a launch too.</li>
 * </ul>
 * The slam and the dive end in their own impact, so the landing does not hurt. While a move runs
 * ({@link #busy}: a slam or dive until it lands, a lunge for {@link #LUNGE_TICKS}), no attack lands
 * and neither hand can be used; the move is the triggering hand's action.
 * <p>
 * The client reads the right click itself ({@code client/UpdraftInput}) and sends an
 * {@link AirMovePayload}: Better Combat swallows vanilla right clicks while the weapon recovers, which
 * is exactly the moment after an Uppercut launch.
 */
public final class Updraft {
	public enum Move {
		SLAM, DIVE, LUNGE
	}

	/** How long a lunge keeps attacks and the other hand blocked; shared with the client. */
	public static final int LUNGE_TICKS = 10;

	/** Downward speed the slam and the dive start at. */
	private static final double PLUNGE_SPEED = 2.2;
	/** Mace slam: damage over a mace smash from the same height, and its reach. */
	private static final float SLAM_POTENCY = 1.5F;
	private static final double SLAM_RADIUS = 0.75;
	/** Sword dive: reach, and the damage added per block fallen, up to a cap. */
	private static final double DIVE_RADIUS = 3.0;
	/** Sword dive: falls this share of the slam's speed, its start and its gravity both. */
	private static final double DIVE_FALL = 0.75;
	private static final Identifier DIVE_GRAVITY = TrimEnchantments.id("updraft_dive");
	private static final float DIVE_PER_BLOCK = 0.1F;
	private static final float DIVE_MAX_FALL = 10.0F;
	/** Spear lunge: speed along the look, the hunger it costs (vanilla Lunge I's). */
	private static final double LUNGE_SPEED = 1.2;
	private static final float LUNGE_EXHAUSTION = 4.0F;
	/** Spear lunge: damage over the spear's own, how far past the player's box it strikes, and the push along the look. */
	private static final float LUNGE_POTENCY = 1.0F;
	private static final double LUNGE_REACH = 0.6;
	private static final double LUNGE_PUSH = 0.6;
	/** Ticks after a launch during which still standing on the ground does not end it. */
	private static final int LAUNCH_GRACE_TICKS = 3;
	/** A plunge that never lands (into the void, say) gives up after this long. */
	private static final int PLUNGE_TICKS = 200;

	private static final class Air {
		final Move move;
		final InteractionHand hand;
		/** Height at the start of the move and the fall already behind the player then. */
		final double startY;
		final double fallBefore;
		final int started;
		/** Lunge: where the player was last tick, and the enemies it already ran through. */
		Vec3 from;
		final Set<Integer> struck = new HashSet<>();

		Air(Move move, InteractionHand hand, ServerPlayer player) {
			this.move = move;
			this.hand = hand;
			this.startY = player.getY();
			this.fallBefore = player.fallDistance;
			this.started = player.tickCount;
			this.from = player.position();
		}

		boolean plunging() {
			return this.move != Move.LUNGE;
		}
	}

	/** Players who used their air move, until they land. */
	private static final Map<ServerPlayer, Air> AIR = new WeakHashMap<>();
	/** Players Updraft launched, by the tick it did, until they land. */
	private static final Map<ServerPlayer, Integer> LAUNCHED = new WeakHashMap<>();

	private Updraft() {
	}

	static void init() {
		PayloadTypeRegistry.serverboundPlay().register(AirMovePayload.TYPE, AirMovePayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(UpdraftLaunchPayload.TYPE, UpdraftLaunchPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(AirMovePayload.TYPE, (payload, context) ->
			start(context.player(), payload.offHand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND));
		// Neither hand is used during a move: the move is the triggering hand's action.
		UseItemCallback.EVENT.register((player, level, hand) ->
			player instanceof ServerPlayer server && busy(server) ? InteractionResult.FAIL : InteractionResult.PASS);
	}

	/** Uppercut sent {@code player} up with Updraft: the spear's lunge, and Lunge itself, open until they land. */
	static void launched(ServerPlayer player) {
		LAUNCHED.put(player, player.tickCount);
		if (ServerPlayNetworking.canSend(player, UpdraftLaunchPayload.TYPE)) {
			ServerPlayNetworking.send(player, UpdraftLaunchPayload.INSTANCE);
		}
	}

	private static boolean launched(ServerPlayer player, int since) {
		Integer at = LAUNCHED.get(player);
		return at != null && player.tickCount - at <= since;
	}

	/** Whether the Lunge enchantment may push {@code player}: on the ground, or in the air after an Updraft launch. */
	public static boolean lungeAllowed(ServerPlayer player) {
		return !airborne(player) || LAUNCHED.containsKey(player);
	}

	private static void start(ServerPlayer player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		Move move = moveFor(player, stack, LAUNCHED.containsKey(player));
		if (move == null || AIR.containsKey(player)) {
			return;
		}
		AIR.put(player, new Air(move, hand, player));
		switch (move) {
			case SLAM, DIVE -> plunge(player, move);
			case LUNGE -> lunge(player);
		}
		stack.hurtAndBreak(1, player, hand);
	}

	/** Whether {@code player} is in a move, during which no attack lands and neither hand is used. */
	public static boolean busy(ServerPlayer player) {
		Air air = AIR.get(player);
		return air != null && (air.plunging() || player.tickCount - air.started <= LUNGE_TICKS);
	}

	/** Whether {@code player} is in the air in a way a move can start from. */
	public static boolean airborne(Player player) {
		return !player.onGround() && !player.isInWater() && !player.isFallFlying() && !player.isPassenger() && !player.getAbilities().flying
			&& !player.isSpectator();
	}

	/**
	 * The move {@code stack} would start for {@code player} now, or null; both sides ask this. The spear
	 * lunges only after an Updraft launch ({@code launched}), and only without the Lunge enchantment,
	 * which does the lunge itself then.
	 */
	public static @Nullable Move moveFor(Player player, ItemStack stack, boolean launched) {
		if (!airborne(player) || !Synergy.UPDRAFT.on(stack)) {
			return null;
		}
		WeaponStyle style = WeaponStyles.of(stack);
		if (style == MaceStyle.INSTANCE) {
			return Move.SLAM;
		}
		if (style == SwordStyle.INSTANCE) {
			return Move.DIVE;
		}
		if (style == SpearStyle.INSTANCE && launched && !hasLunge(stack)) {
			return Move.LUNGE;
		}
		return null;
	}

	private static boolean hasLunge(ItemStack stack) {
		for (Holder<Enchantment> enchantment : stack.getEnchantments().keySet()) {
			if (enchantment.is(Enchantments.LUNGE)) {
				return true;
			}
		}
		return false;
	}

	private static void plunge(ServerPlayer player, Move move) {
		player.setDeltaMovement(0.0, -PLUNGE_SPEED * (move == Move.DIVE ? DIVE_FALL : 1.0), 0.0);
		player.hurtMarked = true;
		if (move == Move.DIVE) {
			AttributeInstance gravity = player.getAttribute(Attributes.GRAVITY);
			if (gravity != null) {
				gravity.addOrUpdateTransientModifier(new AttributeModifier(DIVE_GRAVITY, DIVE_FALL - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
			}
		}
		ServerLevel world = player.level();
		Particles.send(world, ParticleTypes.GUST_EMITTER_SMALL, player.getX(), player.getY() + 0.5, player.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
		world.playSound((Entity) null, player.getX(), player.getY(), player.getZ(),
			move == Move.SLAM ? SoundEvents.MACE_SMASH_AIR : SoundEvents.BREEZE_JUMP, SoundSource.PLAYERS, 1.0F, 1.0F);
	}

	private static void lunge(ServerPlayer player) {
		player.setDeltaMovement(player.getLookAngle().scale(LUNGE_SPEED));
		player.hurtMarked = true;
		player.causeFoodExhaustion(LUNGE_EXHAUSTION);
		ServerLevel world = player.level();
		Particles.send(world, ParticleTypes.GUST_EMITTER_SMALL, player.getX(), player.getY() + 0.5, player.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
		world.playSound((Entity) null, player.getX(), player.getY(), player.getZ(), SoundEvents.LUNGE_2.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
	}

	/** Lands every player who used their move: a slam or a dive that reached the ground strikes there. */
	static void tick() {
		// The launch tick itself can still read as grounded, so a launch only ends on a landing after it.
		LAUNCHED.keySet().removeIf(player -> player.isRemoved() || !player.isAlive()
			|| !launched(player, LAUNCH_GRACE_TICKS) && !airborne(player));
		if (AIR.isEmpty()) {
			return;
		}
		for (Map.Entry<ServerPlayer, Air> entry : new ArrayList<>(AIR.entrySet())) {
			ServerPlayer player = entry.getKey();
			Air air = entry.getValue();
			if (player.isRemoved() || !player.isAlive()) {
				AIR.remove(player);
				endDive(player);
				continue;
			}
			if (air.move == Move.LUNGE && player.tickCount - air.started <= LUNGE_TICKS) {
				lungeHits(player, air);
			}
			boolean landed = player.onGround() || player.isInWater() || player.isFallFlying() || player.isPassenger();
			if (!landed && !(air.plunging() && player.tickCount - air.started > PLUNGE_TICKS)) {
				continue;
			}
			AIR.remove(player);
			endDive(player);
			if (air.plunging() && player.onGround()) {
				impact(player, air);
			}
		}
	}

	/** Gives back the gravity a dive took. */
	private static void endDive(ServerPlayer player) {
		AttributeInstance gravity = player.getAttribute(Attributes.GRAVITY);
		if (gravity != null) {
			gravity.removeModifier(DIVE_GRAVITY);
		}
	}

	/** Strikes every enemy the lunge ran through since last tick, each once per lunge, and carries it along. */
	private static void lungeHits(ServerPlayer player, Air air) {
		ServerLevel world = player.level();
		Vec3 to = player.position();
		AABB path = player.getBoundingBox().move(air.from.subtract(to)).minmax(player.getBoundingBox()).inflate(LUNGE_REACH);
		air.from = to;
		ItemStack weapon = player.getItemInHand(air.hand);
		DamageSource source = world.damageSources().playerAttack(player);
		float base = baseDamage(player, air.hand, weapon) * LUNGE_POTENCY;
		Vec3 look = player.getLookAngle();
		for (LivingEntity enemy : world.getEntitiesOfClass(LivingEntity.class, path,
			e -> e instanceof Enemy && Targets.harmable(player, e) && !air.struck.contains(e.getId()))) {
			air.struck.add(enemy.getId());
			enemy.invulnerableTime = 0;
			if (enemy.hurtServer(world, source, EnchantmentHelper.modifyDamage(world, weapon, enemy, source, base))) {
				EnchantmentHelper.doPostAttackEffectsWithItemSource(world, enemy, source, weapon);
				enemy.setDeltaMovement(enemy.getDeltaMovement().add(look.x * LUNGE_PUSH, 0.15, look.z * LUNGE_PUSH));
				enemy.hurtMarked = true;
				Particles.send(world, ParticleTypes.CRIT, enemy.getX(), enemy.getY(0.5), enemy.getZ(), 8, 0.2, 0.3, 0.2, 0.25);
				world.playSound((Entity) null, enemy.getX(), enemy.getY(), enemy.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0F, 1.1F);
			}
		}
	}

	/** Whether {@code source} is the landing of a slam or a dive, which does not hurt. */
	static boolean forgivesFall(ServerPlayer player, DamageSource source) {
		Air air = AIR.get(player);
		return air != null && air.plunging() && source.is(DamageTypeTags.IS_FALL);
	}

	private static void impact(ServerPlayer player, Air air) {
		ServerLevel world = player.level();
		ItemStack weapon = player.getItemInHand(air.hand);
		float fall = (float) Math.max(0.0, air.fallBefore + air.startY - player.getY());
		boolean slam = air.move == Move.SLAM;
		double radius = slam ? SLAM_RADIUS : DIVE_RADIUS;
		Vec3 center = player.position();
		DamageSource source = world.damageSources().playerAttack(player);
		float base = baseDamage(player, air.hand, weapon);
		// The slam reaches from the player's edge, so its short reach still hits what the player lands among.
		List<LivingEntity> hit = slam ? Targets.enemiesAround(player, SLAM_RADIUS, 1.0) : Targets.enemiesNear(player, center, radius, null);
		for (LivingEntity enemy : hit) {
			float damage = EnchantmentHelper.modifyDamage(world, weapon, enemy, source, base);
			if (slam) {
				damage = (damage + smashBonus(world, weapon, enemy, source, fall)) * SLAM_POTENCY;
			} else {
				damage *= 1.0F + DIVE_PER_BLOCK * Math.min(fall, DIVE_MAX_FALL);
			}
			enemy.invulnerableTime = 0;
			if (enemy.hurtServer(world, source, damage)) {
				Vec3 away = enemy.position().subtract(center);
				Vec3 flat = new Vec3(away.x, 0.0, away.z);
				double push = (slam ? 0.9 : 0.5) * (1.0 - Math.min(flat.length() / radius, 1.0)) + 0.2;
				flat = flat.lengthSqr() < 1.0E-4 ? Vec3.ZERO : flat.normalize().scale(push);
				enemy.setDeltaMovement(enemy.getDeltaMovement().add(flat.x, slam ? 0.4 : 0.25, flat.z));
				enemy.hurtMarked = true;
			}
		}
		player.resetFallDistance();
		BlockState floor = world.getBlockState(player.blockPosition().below());
		if (!floor.isAir()) {
			Particles.send(world, new BlockParticleOption(ParticleTypes.BLOCK, floor), center.x, center.y + 0.1, center.z, slam ? 40 : 24,
				radius * 0.4, 0.05, radius * 0.4, 0.15);
		}
		if (slam) {
			Particles.send(world, ParticleTypes.EXPLOSION, center.x, center.y + 0.2, center.z, 1, 0.0, 0.0, 0.0, 0.0);
			world.playSound((Entity) null, center.x, center.y, center.z, SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.PLAYERS, 1.0F, 1.0F);
		} else {
			for (int i = 0; i < 8; i++) {
				double angle = Math.PI * 2.0 * i / 8.0;
				Particles.send(world, ParticleTypes.SWEEP_ATTACK, center.x + Math.cos(angle) * radius * 0.6, center.y + 0.3,
					center.z + Math.sin(angle) * radius * 0.6, 1, 0.0, 0.0, 0.0, 0.0);
			}
			world.playSound((Entity) null, center.x, center.y, center.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.8F);
			world.playSound((Entity) null, center.x, center.y, center.z, SoundEvents.MACE_SMASH_GROUND, SoundSource.PLAYERS, 0.8F, 1.2F);
		}
	}

	/**
	 * The weapon's attack damage: the player's attribute for the main hand; for the off hand, the
	 * player's base plus the weapon's own flat bonus, as if it were held in the main hand.
	 */
	private static float baseDamage(ServerPlayer player, InteractionHand hand, ItemStack weapon) {
		if (hand == InteractionHand.MAIN_HAND) {
			return (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
		}
		double[] damage = {player.getAttributeBaseValue(Attributes.ATTACK_DAMAGE)};
		weapon.forEachModifier(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
			if (attribute.is(Attributes.ATTACK_DAMAGE) && modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
				damage[0] += modifier.amount();
			}
		});
		return (float) damage[0];
	}

	/** A mace smash's bonus from {@code fall} blocks, as vanilla's, with Density. */
	private static float smashBonus(ServerLevel world, ItemStack mace, Entity target, DamageSource source, float fall) {
		if (fall <= 1.5F) {
			return 0.0F;
		}
		float bonus;
		if (fall <= 3.0F) {
			bonus = 4.0F * fall;
		} else if (fall <= 8.0F) {
			bonus = 12.0F + 2.0F * (fall - 3.0F);
		} else {
			bonus = 22.0F + fall - 8.0F;
		}
		return bonus + EnchantmentHelper.modifyFallBasedDamage(world, mace, target, source, 0.0F) * fall;
	}
}
