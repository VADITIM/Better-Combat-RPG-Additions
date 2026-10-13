package dev.vadim.bcenchantments.perk;

import dev.vadim.bcenchantments.combat.SwingResult;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SimpleExplosionDamageCalculator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Calls lightning down on the target when a Lightning combo completes: two full attack cycles, a little shortened. */
public final class Lightning implements ChargedPerk {
	static final Lightning INSTANCE = new Lightning();

	/** Lightning fires after this many full attack cycles minus {@link #HIT_DISCOUNT} hits, never under {@link #MIN_HITS}. */
	private static final int CYCLES = 2;
	private static final int HIT_DISCOUNT = 2;
	private static final int MIN_HITS = 2;
	/** Damage in health points (2 per heart) for levels 1 to 3. */
	private static final float[] DAMAGE = {7.0F, 13.0F, 17.0F};
	private static final float BURST_RADIUS = 1.2F;
	/** Sideways speed of the level 3 burst's push. */
	private static final double BURST_PUSH = 0.6;
	/** Upward speed of the push; with gravity and drag this lifts an entity about one block at most. */
	private static final double BURST_LIFT = 0.36;

	private Lightning() {
	}

	@Override
	public String name() {
		return "lightning";
	}

	@Override
	public ResourceKey<Enchantment> enchantment() {
		return Perks.LIGHTNING;
	}

	@Override
	public int required(int attacks) {
		return Math.max(MIN_HITS, CYCLES * Math.max(1, attacks) - HIT_DISCOUNT);
	}

	@Override
	public int graceTicks() {
		return BREAKS_WITH_COMBO;
	}

	@Override
	public int advance(int charge, SwingResult swing) {
		return charge + 1;
	}

	@Override
	public void fire(SwingResult swing, int level) {
		strike(swing.player(), swing.primary(), level);
	}

	private static void strike(ServerPlayer player, LivingEntity target, int level) {
		level = Math.min(level, DAMAGE.length);
		ServerLevel world = player.level();
		LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(world, EntitySpawnReason.TRIGGERED);
		if (bolt != null) {
			// Visual only: no fire, no charged creepers, and the player standing next to it is spared.
			bolt.snapTo(target.position());
			bolt.setVisualOnly(true);
			bolt.setCause(player);
			world.addFreshEntity(bolt);
		}

		DamageSource source = new DamageSource(
			world.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.LIGHTNING_BOLT),
			player
		);
		// The finishing slash just hit, so drop the hurt cooldown or most of the bolt would be swallowed.
		target.invulnerableTime = 0;
		target.hurtServer(world, source, DAMAGE[level - 1]);

		if (level >= 3) {
			burst(world, player, target);
		}
	}

	/**
	 * A small wind-charge-style burst at the target. The explosion is only there for its gust particles:
	 * it is silent and pushes nothing, and the push is applied here so the lift can be capped.
	 */
	private static void burst(ServerLevel world, ServerPlayer player, LivingEntity target) {
		SimpleExplosionDamageCalculator visual = new SimpleExplosionDamageCalculator(false, false, Optional.of(0.0F), Optional.empty());
		world.explode(
			player, null, visual,
			target.getX(), target.getY(), target.getZ(),
			BURST_RADIUS, false, Level.ExplosionInteraction.TRIGGER,
			ParticleTypes.GUST_EMITTER_SMALL, ParticleTypes.GUST_EMITTER_LARGE,
			WeightedList.of(), Holder.direct(SoundEvents.EMPTY)
		);

		AABB area = target.getBoundingBox().inflate(BURST_RADIUS);
		for (LivingEntity entity : world.getEntitiesOfClass(LivingEntity.class, area, e -> e != player && e.isAlive())) {
			// Away from the player for the target itself, away from the target for anything beside it.
			Vec3 from = entity == target ? player.position() : target.position();
			Vec3 away = entity.position().subtract(from).multiply(1.0, 0.0, 1.0);
			away = away.lengthSqr() < 1.0E-4 ? Vec3.ZERO : away.normalize().scale(BURST_PUSH);
			Vec3 motion = entity.getDeltaMovement();
			entity.setDeltaMovement(motion.x + away.x, Math.min(Math.max(motion.y, BURST_LIFT), BURST_LIFT), motion.z + away.z);
			entity.hurtMarked = true;
		}
	}
}
