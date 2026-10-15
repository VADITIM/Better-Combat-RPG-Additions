package dev.vadim.bcenchantments.perk;

import dev.vadim.bcenchantments.Particles;
import dev.vadim.bcenchantments.mob.Adaptation;
import java.util.function.Consumer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A wind-charge-style burst: a wind charge's gust emitter, with the push applied here so the lift can
 * be capped.
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
		at(player, at, radius, damage, origin, also, false);
	}

	/** As above; with {@code awayFromPlayer} everything caught is pushed away from the player, not from the burst. */
	static void at(ServerPlayer player, Vec3 at, double radius, float damage, LivingEntity origin, Consumer<LivingEntity> also, boolean awayFromPlayer) {
		ServerLevel world = player.level();
		// The emitter a wind charge's explosion of this radius would show.
		Particles.send(world, radius < 2.0 ? ParticleTypes.GUST_EMITTER_SMALL : ParticleTypes.GUST_EMITTER_LARGE,
			at.x, at.y, at.z, 1, 0.0, 0.0, 0.0, 0.0);
		if (damage > 0.0F) {
			world.playSound((Entity) null, at.x, at.y, at.z, SoundEvents.WIND_CHARGE_BURST, SoundSource.PLAYERS, 0.8F, 1.1F);
		}

		AABB area = new AABB(at, at).inflate(radius);
		for (LivingEntity entity : world.getEntitiesOfClass(LivingEntity.class, area, e -> Targets.harmable(player, e))) {
			boolean countered = Adaptation.resists(entity, Perks.WIND_CHARGED);
			if (damage > 0.0F) {
				entity.invulnerableTime = 0;
				entity.hurtServer(world, world.damageSources().windCharge(player, player), countered ? Adaptation.reduced(damage) : damage);
			}
			if (countered) {
				// Anchored: the gust passes it by.
				also.accept(entity);
				continue;
			}
			throwFrom(entity, entity == origin || awayFromPlayer ? player.position() : at, 1.0);
			also.accept(entity);
		}
	}

	/**
	 * A small gust on one enemy: it is thrown away from the player at {@code share} of a burst's push,
	 * unless it counters Wind Charged.
	 */
	static void gust(ServerPlayer player, LivingEntity target, double share) {
		if (!target.isAlive() || Adaptation.resists(target, Perks.WIND_CHARGED)) {
			return;
		}
		throwFrom(target, player.position(), share);
		Particles.send(player.level(), ParticleTypes.GUST, target.getX(), target.getY(0.5), target.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
		player.level().playSound((Entity) null, target.getX(), target.getY(), target.getZ(), SoundEvents.BREEZE_SHOOT, SoundSource.PLAYERS, 0.5F, 1.4F);
	}

	/** Pushes {@code entity} sideways away from {@code from} at {@code share} of the push, lifting it a little. */
	private static void throwFrom(LivingEntity entity, Vec3 from, double share) {
		Vec3 away = entity.position().subtract(from).multiply(1.0, 0.0, 1.0);
		away = away.lengthSqr() < 1.0E-4 ? Vec3.ZERO : away.normalize().scale(PUSH * share);
		Vec3 motion = entity.getDeltaMovement();
		double lift = LIFT * Math.min(share, 1.0);
		entity.setDeltaMovement(motion.x + away.x, Math.min(Math.max(motion.y, lift), LIFT), motion.z + away.z);
		entity.hurtMarked = true;
	}
}
