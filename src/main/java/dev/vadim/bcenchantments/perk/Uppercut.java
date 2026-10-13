package dev.vadim.bcenchantments.perk;

import dev.vadim.bcenchantments.combat.SwingResult;
import dev.vadim.bcenchantments.weapon.WeaponStyles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Throws everything a launching attack hits into the air. Which attacks launch is up to each weapon
 * style: the sword's uppercut, the spear's heavy thrust.
 */
final class Uppercut {
	/** Upward speed given to targets for levels 1 to 3. */
	private static final double[] LAUNCH = {0.55, 0.7, 0.85};

	private Uppercut() {
	}

	static void onSwing(SwingResult swing) {
		if (!WeaponStyles.launches(swing.hand())) {
			return;
		}
		int level = Math.min(Perks.level(swing.weapon(), Perks.UPPERCUT), LAUNCH.length);
		if (level <= 0) {
			return;
		}
		ServerLevel world = swing.player().level();
		for (LivingEntity target : swing.hits()) {
			Vec3 motion = target.getDeltaMovement();
			target.setDeltaMovement(motion.x * 0.3, Math.max(motion.y, LAUNCH[level - 1]), motion.z * 0.3);
			target.hurtMarked = true;
			world.sendParticles(ParticleTypes.POOF, target.getX(), target.getY(), target.getZ(), 6, 0.3, 0.05, 0.3, 0.02);
			world.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(0.5), target.getZ(), 8, 0.2, 0.4, 0.2, 0.25);
		}
		world.playSound((Entity) null, swing.player().getX(), swing.player().getY(), swing.player().getZ(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.0F, 0.8F);
	}
}
