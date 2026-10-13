package dev.vadim.bcenchantments.perk;

import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SimpleExplosionDamageCalculator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A wind-charge-style burst. The explosion is only there for its gust particles: it is silent and
 * pushes nothing, and the push is applied here so the lift can be capped.
 */
final class WindBurst {
	/** Sideways speed of the push. */
	private static final double PUSH = 0.6;
	/** Upward speed of the push; with gravity and drag this lifts an entity about one block at most. */
	private static final double LIFT = 0.36;

	private WindBurst() {
	}

	static void at(ServerPlayer player, Vec3 at, double radius, float damage, LivingEntity origin) {
		at(player, at, radius, damage, origin, entity -> {
		});
	}

	/**
	 * Bursts at {@code at}, hurting and pushing away everything the player may harm within
	 * {@code radius}. {@code origin}, if given, is pushed away from the player instead of from the
	 * burst, so a struck target is thrown onward. {@code also} runs on everything caught.
	 */
	static void at(ServerPlayer player, Vec3 at, double radius, float damage, LivingEntity origin, Consumer<LivingEntity> also) {
		ServerLevel world = player.level();
		SimpleExplosionDamageCalculator visual = new SimpleExplosionDamageCalculator(false, false, Optional.of(0.0F), Optional.empty());
		world.explode(
			player, null, visual,
			at.x, at.y, at.z,
			(float) radius, false, Level.ExplosionInteraction.TRIGGER,
			ParticleTypes.GUST_EMITTER_SMALL, ParticleTypes.GUST_EMITTER_LARGE,
			WeightedList.of(), Holder.direct(SoundEvents.EMPTY)
		);
		if (damage > 0.0F) {
			world.playSound((Entity) null, at.x, at.y, at.z, SoundEvents.WIND_CHARGE_BURST, SoundSource.PLAYERS, 0.8F, 1.1F);
		}

		AABB area = new AABB(at, at).inflate(radius);
		for (LivingEntity entity : world.getEntitiesOfClass(LivingEntity.class, area, e -> Targets.harmable(player, e))) {
			if (damage > 0.0F) {
				entity.invulnerableTime = 0;
				entity.hurtServer(world, world.damageSources().windCharge(player, player), damage);
			}
			Vec3 from = entity == origin ? player.position() : at;
			Vec3 away = entity.position().subtract(from).multiply(1.0, 0.0, 1.0);
			away = away.lengthSqr() < 1.0E-4 ? Vec3.ZERO : away.normalize().scale(PUSH);
			Vec3 motion = entity.getDeltaMovement();
			entity.setDeltaMovement(motion.x + away.x, Math.min(Math.max(motion.y, LIFT), LIFT), motion.z + away.z);
			entity.hurtMarked = true;
			also.accept(entity);
		}
	}
}
