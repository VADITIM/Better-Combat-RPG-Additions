package dev.vadim.bcenchantments.perk;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Knockback damage: an enemy thrown hard that slams into a wall before it slows down takes a hit,
 * credited to the player who threw it.
 */
final class Impact {
	/** Sideways speed, in blocks per tick, an enemy must still have had to be hurt by the wall. */
	private static final double MIN_SPEED = 0.35;

	private static final class Thrown {
		final ServerPlayer player;
		final float damage;
		final long until;
		double speed;

		Thrown(ServerPlayer player, float damage, long until, double speed) {
			this.player = player;
			this.damage = damage;
			this.until = until;
			this.speed = speed;
		}
	}

	private static final Map<LivingEntity, Thrown> THROWN = new WeakHashMap<>();
	private static long now;

	private Impact() {
	}

	/** Watches {@code target} for {@code ticks}: if it hits a wall while still flying, it takes {@code damage}. */
	static void track(LivingEntity target, ServerPlayer player, float damage, int ticks) {
		THROWN.put(target, new Thrown(player, damage, now + ticks, target.getDeltaMovement().horizontalDistance()));
	}

	static void tick() {
		now++;
		Iterator<Map.Entry<LivingEntity, Thrown>> it = THROWN.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<LivingEntity, Thrown> entry = it.next();
			LivingEntity target = entry.getKey();
			Thrown thrown = entry.getValue();
			if (target.isRemoved() || !target.isAlive() || now > thrown.until || thrown.player.isRemoved()) {
				it.remove();
				continue;
			}
			// The collision has already stopped it, so the speed it hit with is last tick's.
			if (target.horizontalCollision && thrown.speed >= MIN_SPEED && target.level() instanceof ServerLevel world) {
				slam(world, target, thrown);
				it.remove();
				continue;
			}
			thrown.speed = target.getDeltaMovement().horizontalDistance();
		}
	}

	private static void slam(ServerLevel world, LivingEntity target, Thrown thrown) {
		DamageSource source = new DamageSource(
			world.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.FLY_INTO_WALL),
			thrown.player
		);
		target.invulnerableTime = 0;
		target.hurtServer(world, source, thrown.damage);

		Vec3 at = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
		// The wall it hit, for the chips of it that fly off.
		BlockState wall = null;
		for (BlockPos near : BlockPos.betweenClosed(BlockPos.containing(at).offset(-1, 0, -1), BlockPos.containing(at).offset(1, 0, 1))) {
			BlockState state = world.getBlockState(near);
			if (!state.getCollisionShape(world, near).isEmpty()) {
				wall = state;
				break;
			}
		}
		if (wall != null) {
			world.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, wall), at.x, at.y, at.z, 20, 0.3, 0.4, 0.3, 0.15);
		}
		world.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 12, 0.3, 0.3, 0.3, 0.3);
		world.playSound((Entity) null, at.x, at.y, at.z, SoundEvents.MACE_SMASH_GROUND, SoundSource.PLAYERS, 0.9F, 1.1F);
	}
}
