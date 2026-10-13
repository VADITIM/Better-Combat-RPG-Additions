package dev.vadim.bcenchantments.perk;

import dev.vadim.bcenchantments.Particles;
import dev.vadim.bcenchantments.progression.Refinement;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The few powers a trim can put on a shield, each set off by a hit the shield blocks and then resting
 * on a cooldown:
 * <ul>
 * <li>Wind Charged: a gust from the shield's face throws back everything in front of it, at 30% of a
 * wind charge's strength. Every 30 seconds.</li>
 * <li>Cold Edge: the attacker whose hit was blocked is chilled hard. Every 10 seconds.</li>
 * </ul>
 */
public final class ShieldPowers {
	private static final int WIND_COOLDOWN_TICKS = 30 * 20;
	/** How far ahead of the shield the gust reaches, and how wide: the cosine of its half angle. */
	private static final double WIND_RANGE = 4.0;
	private static final double WIND_CONE = 0.5;
	/** A thrown wind charge pushes with a strength of 1.22; the gust has 30% of that. */
	private static final double WIND_PUSH = 1.22 * 0.3;
	private static final double WIND_LIFT = 0.2;

	private static final int FROST_COOLDOWN_TICKS = 10 * 20;
	/** Cold Edge level the chill counts as. */
	private static final int FROST_LEVEL = 3;
	/** Only attackers this close are chilled, so a blocked arrow does not chill the archer. */
	private static final double FROST_RANGE = 5.0;

	/** Game time each player's shield powers are ready again. */
	private static final Map<ServerPlayer, Long> WIND_READY = new WeakHashMap<>();
	private static final Map<ServerPlayer, Long> FROST_READY = new WeakHashMap<>();

	private ShieldPowers() {
	}

	/** After {@code defender} blocked a hit from {@code source} with {@code shield}. */
	public static void onBlock(LivingEntity defender, DamageSource source, ItemStack shield) {
		if (!(defender instanceof ServerPlayer player) || shield == null || !Refinement.isShield(shield)) {
			return;
		}
		long now = player.level().getGameTime();
		if (Perks.level(shield, Perks.WIND_CHARGED) > 0 && ready(WIND_READY, player, now, WIND_COOLDOWN_TICKS)) {
			gust(player);
		}
		if (Perks.level(shield, Perks.COLD_EDGE) > 0 && source.getEntity() instanceof LivingEntity attacker
			&& attacker.distanceTo(player) <= FROST_RANGE && ready(FROST_READY, player, now, FROST_COOLDOWN_TICKS)) {
			ColdEdge.chill(attacker, FROST_LEVEL);
		}
	}

	private static boolean ready(Map<ServerPlayer, Long> readyAt, ServerPlayer player, long now, int cooldown) {
		Long at = readyAt.get(player);
		if (at != null && now < at) {
			return false;
		}
		readyAt.put(player, now + cooldown);
		return true;
	}

	private static void gust(ServerPlayer player) {
		ServerLevel world = player.level();
		Vec3 facing = player.getLookAngle().multiply(1.0, 0.0, 1.0);
		facing = facing.lengthSqr() < 1.0E-4 ? Vec3.ZERO : facing.normalize();
		Vec3 front = player.position().add(facing.scale(1.0)).add(0.0, player.getBbHeight() * 0.5, 0.0);
		Vec3 at = player.position();
		AABB area = new AABB(at, at).inflate(WIND_RANGE);
		for (LivingEntity entity : world.getEntitiesOfClass(LivingEntity.class, area, e -> Targets.harmable(player, e))) {
			Vec3 away = entity.position().subtract(at).multiply(1.0, 0.0, 1.0);
			double distance = away.length();
			if (distance > WIND_RANGE || distance > 1.0E-3 && away.scale(1.0 / distance).dot(facing) < WIND_CONE) {
				continue;
			}
			Vec3 push = distance > 1.0E-3 ? away.scale(WIND_PUSH / distance) : facing.scale(WIND_PUSH);
			Vec3 motion = entity.getDeltaMovement();
			entity.setDeltaMovement(motion.x + push.x, Math.max(motion.y, WIND_LIFT), motion.z + push.z);
			entity.hurtMarked = true;
		}
		Particles.send(world, ParticleTypes.GUST_EMITTER_SMALL, front.x, front.y, front.z, 1, 0.0, 0.0, 0.0, 0.0);
		Particles.send(world, ParticleTypes.GUST, front.x, front.y, front.z, 6, 0.6, 0.4, 0.6, 0.0);
		world.playSound((Entity) null, front.x, front.y, front.z, SoundEvents.WIND_CHARGE_BURST, SoundSource.PLAYERS, 0.8F, 1.2F);
	}
}
