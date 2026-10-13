package dev.vadim.bcenchantments.perk;

import dev.vadim.bcenchantments.combat.SwingResult;
import dev.vadim.bcenchantments.weapon.WeaponStyles;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.phys.Vec3;

/**
 * Throws everything a launching attack hits into the air. Which attacks launch is up to each weapon
 * style: the sword's uppercut, the spear's heavy thrust. With {@link Synergy#UPDRAFT} the targets and
 * the player go straight up, the player topping out a block above the struck target, which hangs
 * stunned in the air to be juggled while the player drifts down slowly. A mace's next swing in the
 * air smashes as if it had fallen six blocks.
 */
public final class Uppercut {
	/** Upward speed given to targets for levels 1 to 3. */
	private static final double[] LAUNCH = {0.55, 0.7, 0.85};
	/** Updraft: ticks the targets rise before they hang, and how long they hang stunned. */
	private static final int RISE_TICKS = 8;
	private static final int HANG_TICKS = 30;
	/** How far above the struck target the player tops out. */
	private static final double PLAYER_ABOVE = 1.0;
	/** Fall distance a mace slam counts during Updraft, and how long after the launch it can land. */
	private static final double SLAM_FALL = 6.0;
	private static final int SLAM_TICKS = RISE_TICKS + HANG_TICKS + 10;
	/** Vanilla gravity and air drag for living entities, to predict how high a launch goes. */
	private static final double GRAVITY = 0.08;
	private static final double DRAG = 0.98;

	/** Players launched by Updraft, until the tick their mace slam runs out. */
	private static final Map<ServerPlayer, Integer> SLAM = new WeakHashMap<>();

	private Uppercut() {
	}

	static void onSwing(SwingResult swing) {
		ServerPlayer player = swing.player();
		// Any landed swing after the launch spends the slam, whether or not it was one.
		boolean launching = WeaponStyles.launches(player, swing.hand());
		if (!launching) {
			SLAM.remove(player);
			return;
		}
		int level = Math.min(Perks.level(swing.weapon(), Perks.UPPERCUT), LAUNCH.length);
		if (level <= 0) {
			return;
		}
		ServerLevel world = player.level();
		boolean updraft = Synergy.UPDRAFT.on(swing.weapon());
		// Updraft sends the player up on its own; Launch only follows the targets without it.
		double speed = Momentum.launch(player, swing.weapon(), LAUNCH[level - 1], !updraft);
		for (LivingEntity target : swing.hits()) {
			if (updraft) {
				target.setDeltaMovement(0.0, speed, 0.0);
				target.hurtMarked = true;
				Control.suspend(target, RISE_TICKS, HANG_TICKS);
			} else {
				launch(target, speed);
			}
			world.sendParticles(ParticleTypes.POOF, target.getX(), target.getY(), target.getZ(), 6, 0.3, 0.05, 0.3, 0.02);
			world.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(0.5), target.getZ(), 8, 0.2, 0.4, 0.2, 0.25);
		}
		if (updraft) {
			LivingEntity target = swing.primary();
			double height = target.getY() + rise(speed, RISE_TICKS) + PLAYER_ABOVE - player.getY();
			player.setDeltaMovement(0.0, speedFor(Math.max(height, 0.5)), 0.0);
			player.hurtMarked = true;
			player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, SLAM_TICKS, 0, false, false, true));
			SLAM.put(player, player.tickCount + SLAM_TICKS);
			world.sendParticles(ParticleTypes.GUST_EMITTER_SMALL, player.getX(), player.getY(), player.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
			world.playSound((Entity) null, player.getX(), player.getY(), player.getZ(), SoundEvents.BREEZE_JUMP, SoundSource.PLAYERS, 1.0F, 1.0F);
		}
		world.playSound((Entity) null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.0F, 0.8F);
	}

	/**
	 * Before each attack: a player in the air after an Updraft launch smashes with a mace as if from
	 * {@link #SLAM_FALL} blocks, so the mace's own fall damage, Density and Breach apply. Slow Falling
	 * keeps the real fall distance at zero, so without this a mace could never smash up there.
	 */
	public static void beforeAttack(ServerPlayer player) {
		Integer until = SLAM.get(player);
		if (until == null || player.tickCount > until || player.onGround()) {
			return;
		}
		if (player.getMainHandItem().getItem() instanceof MaceItem) {
			player.fallDistance = Math.max(player.fallDistance, SLAM_FALL);
		}
	}

	private static void launch(LivingEntity entity, double speed) {
		Vec3 motion = entity.getDeltaMovement();
		entity.setDeltaMovement(motion.x * 0.3, Math.max(motion.y, speed), motion.z * 0.3);
		entity.hurtMarked = true;
	}

	/** How far an entity thrown up at {@code speed} rises within {@code ticks}, stopping at the top. */
	private static double rise(double speed, int ticks) {
		double y = 0.0;
		for (int i = 0; i < ticks && speed > 0.0; i++) {
			y += speed;
			speed = (speed - GRAVITY) * DRAG;
		}
		return y;
	}

	/** The upward speed that tops out {@code height} blocks up. */
	private static double speedFor(double height) {
		double low = 0.0;
		double high = 3.0;
		for (int i = 0; i < 30; i++) {
			double mid = (low + high) * 0.5;
			if (rise(mid, 200) < height) {
				low = mid;
			} else {
				high = mid;
			}
		}
		return high;
	}
}
