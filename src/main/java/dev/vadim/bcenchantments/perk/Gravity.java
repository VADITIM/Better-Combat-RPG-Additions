package dev.vadim.bcenchantments.perk;

import dev.vadim.bcenchantments.Particles;
import dev.vadim.bcenchantments.combat.SwingResult;
import dev.vadim.bcenchantments.mob.Adaptation;
import java.util.function.Predicate;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Every landed swing drags the enemies around the struck target toward it, gathering a crowd for the
 * next swing, like the Dungeons enchantment. Synergies pull with it elsewhere:
 * <ul>
 * <li>{@link Synergy#SINGULARITY}: a Shockwave drags the enemies beside its path into it.</li>
 * <li>{@link Synergy#BLACK_HOLE}: every enemy Chains binds pulls the enemies around it in.</li>
 * <li>{@link Synergy#CYCLONE}: a kill draws the enemies around it in before its wind burst goes off.</li>
 * </ul>
 */
public final class Gravity {
	/** Pull radius around the target and pull speed for levels 1 to 3. */
	private static final double[] RADIUS = {3.0, 4.0, 5.0};
	private static final double[] PULL = {0.35, 0.45, 0.55};
	/** Share of that pull a plain landed swing uses; the synergies pull with the full one. */
	private static final double SWING_SHARE = 0.5;
	/** Enemies closer than this stay where they are, so a pull never stacks them inside the target. */
	private static final double MIN_DISTANCE = 1.2;

	private Gravity() {
	}

	static void onSwing(SwingResult swing) {
		int level = level(swing);
		if (level <= 0) {
			return;
		}
		LivingEntity target = swing.primary();
		Adaptation.used(swing.player(), swing.weapon(), Perks.GRAVITY);
		pull(swing.player(), target.position(), RADIUS[level - 1], PULL[level - 1] * SWING_SHARE, enemy -> enemy == target, true);
	}

	private static int level(SwingResult swing) {
		return Math.min(Perks.level(swing.weapon(), Perks.GRAVITY), RADIUS.length);
	}

	/** Pull speed for {@code level}, for the synergies. */
	static double strength(int level) {
		return PULL[Math.clamp(level, 1, PULL.length) - 1];
	}

	/**
	 * Drags the enemies within {@code radius} of {@code center}, leaving out those {@code skip} names,
	 * toward it at {@code speed}, with the swirl of an end portal; {@code loud} adds the sound and the
	 * swirl at the centre, for a pull that happens once rather than every tick.
	 */
	static void pull(ServerPlayer player, Vec3 center, double radius, double speed, Predicate<LivingEntity> skip, boolean loud) {
		ServerLevel world = player.level();
		boolean pulled = false;
		for (LivingEntity enemy : Targets.enemiesNear(player, center, radius, null)) {
			if (!skip.test(enemy)) {
				pulled |= drag(world, enemy, center, speed);
			}
		}
		if (pulled && loud) {
			Particles.send(world, ParticleTypes.PORTAL, center.x, center.y + 0.8, center.z, 16, 0.3, 0.4, 0.3, 0.6);
			world.playSound((Entity) null, center.x, center.y, center.z, SoundEvents.ENDER_EYE_DEATH, SoundSource.PLAYERS, 0.6F, 0.6F);
		}
	}

	/** Drags {@code enemy} toward {@code center} at {@code speed}; false if it is already there. */
	private static boolean drag(ServerLevel world, LivingEntity enemy, Vec3 center, double speed) {
		Vec3 toward = center.subtract(enemy.position()).multiply(1.0, 0.0, 1.0);
		if (toward.lengthSqr() < MIN_DISTANCE * MIN_DISTANCE || Adaptation.resists(enemy, Perks.GRAVITY)) {
			return false;
		}
		Vec3 motion = toward.normalize().scale(speed);
		enemy.setDeltaMovement(motion.x, Math.max(enemy.getDeltaMovement().y, 0.1), motion.z);
		enemy.hurtMarked = true;
		Particles.send(world, ParticleTypes.REVERSE_PORTAL, enemy.getX(), enemy.getY(0.5), enemy.getZ(), 6, 0.2, 0.3, 0.2, 0.05);
		return true;
	}
}
