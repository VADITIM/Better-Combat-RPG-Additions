package dev.vadim.bcenchantments.perk;

import java.util.Comparator;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Who a perk's area effects may touch. */
final class Targets {
	private Targets() {
	}

	/** Anything living the player's effects may hurt: not the player, their allies, their pets or their vexes. */
	static boolean harmable(ServerPlayer player, LivingEntity target) {
		return target != player
			&& target.isAlive()
			&& !target.isSpectator()
			&& !target.isAlliedTo(player)
			&& !(target instanceof TamableAnimal pet && pet.isOwnedBy(player))
			&& !GuardianStrike.guardian(target);
	}

	/** Hostile mobs within {@code radius} of {@code center}, nearest first, leaving out {@code except}. */
	static List<LivingEntity> enemiesNear(ServerPlayer player, Vec3 center, double radius, LivingEntity except) {
		AABB box = new AABB(center, center).inflate(radius);
		double radiusSqr = radius * radius;
		List<LivingEntity> found = player.level().getEntitiesOfClass(LivingEntity.class, box,
			e -> e != except && e instanceof Enemy && harmable(player, e) && e.distanceToSqr(center) <= radiusSqr);
		found.sort(Comparator.comparingDouble(e -> e.distanceToSqr(center)));
		return found;
	}

	/**
	 * Hostile mobs whose box comes within {@code reach} of the player's own box sideways ({@code below} down,
	 * a little up), nearest first. Measured edge to edge, so a short reach still hits what stands against the player.
	 */
	static List<LivingEntity> enemiesAround(ServerPlayer player, double reach, double below) {
		AABB own = player.getBoundingBox();
		AABB box = new AABB(own.minX - reach, own.minY - below, own.minZ - reach, own.maxX + reach, own.maxY + 0.5, own.maxZ + reach);
		Vec3 center = player.position();
		List<LivingEntity> found = player.level().getEntitiesOfClass(LivingEntity.class, box,
			e -> e instanceof Enemy && harmable(player, e));
		found.sort(Comparator.comparingDouble(e -> e.distanceToSqr(center)));
		return found;
	}
}
