package dev.vadim.bcenchantments.perk;

import dev.vadim.bcenchantments.Particles;
import dev.vadim.bcenchantments.combat.SwingResult;
import dev.vadim.bcenchantments.weapon.WeaponStyles;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Throws everything a launching attack hits into the air. Which attacks launch is up to each weapon
 * style: the sword's uppercut, the spear's heavy thrust. With {@link Synergy#UPDRAFT} the targets and
 * the player go straight up, the player topping out twice as high as a block above the struck target,
 * both falling normally; up there, right click does the weapon's air move ({@link Updraft}).
 */
public final class Uppercut {
	/** Upward speed given to targets for levels 1 to 3. */
	private static final double[] LAUNCH = {0.55, 0.7, 0.85};
	/** Updraft: ticks the targets are counted as rising to set the player's height. */
	private static final int RISE_TICKS = 8;
	/** How far above the struck target the player tops out. */
	private static final double PLAYER_ABOVE = 1.0;
	/** The player's launch height, as a multiple of the height that would top out above the target. */
	private static final double PLAYER_HEIGHT = 2.0;
	/** Vanilla gravity and air drag for living entities, to predict how high a launch goes. */
	private static final double GRAVITY = 0.08;
	private static final double DRAG = 0.98;

	/** Players launched by Updraft, until the tick their fall damage stops being forgiven. */
	private static final Map<ServerPlayer, Integer> FALL_SAFE = new WeakHashMap<>();
	private static final int FALL_SAFE_TICKS = 600;

	private Uppercut() {
	}

	static void onSwing(SwingResult swing) {
		ServerPlayer player = swing.player();
		if (!WeaponStyles.launches(player, swing.hand())) {
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
			} else {
				launch(target, speed);
			}
			Particles.send(world, ParticleTypes.POOF, target.getX(), target.getY(), target.getZ(), 6, 0.3, 0.05, 0.3, 0.02);
			Particles.send(world, ParticleTypes.CRIT, target.getX(), target.getY(0.5), target.getZ(), 8, 0.2, 0.4, 0.2, 0.25);
		}
		if (updraft) {
			LivingEntity target = swing.primary();
			double height = target.getY() + rise(speed, RISE_TICKS) + PLAYER_ABOVE - player.getY();
			// Twice the height; nothing hangs, both fall normally.
			player.setDeltaMovement(0.0, speedFor(Math.max(height, 0.5) * PLAYER_HEIGHT), 0.0);
			player.hurtMarked = true;
			FALL_SAFE.put(player, player.tickCount + FALL_SAFE_TICKS);
			Updraft.launched(player);
			Particles.send(world, ParticleTypes.GUST_EMITTER_SMALL, player.getX(), player.getY(), player.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
			world.playSound((Entity) null, player.getX(), player.getY(), player.getZ(), SoundEvents.BREEZE_JUMP, SoundSource.PLAYERS, 1.0F, 1.0F);
		}
		world.playSound((Entity) null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.0F, 0.8F);
	}

	/** Whether {@code source} is the fall of a player Updraft threw up, which does not hurt; ends the protection. */
	public static boolean forgivesFall(ServerPlayer player, DamageSource source) {
		Integer until = FALL_SAFE.get(player);
		if (until == null || !source.is(DamageTypeTags.IS_FALL)) {
			return false;
		}
		FALL_SAFE.remove(player);
		return player.tickCount <= until;
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
