package dev.vadim.bcenchantments.perk;

import dev.vadim.bcenchantments.combat.SwingResult;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Calls lightning down on the target when a Lightning combo completes: two full attack cycles, a
 * little shortened. With {@link Synergy#CHARGED_LIGHTNING} it bursts like a wind charge where it strikes.
 * With {@link Synergy#SHATTER}, any of the weapon's lightning that hits an enemy already frozen solid
 * (taking freeze damage) deals double damage and splashes a short chill around it; one that is only
 * slowed by Cold Edge takes 15% more. A kill with a Shatter weapon spreads its chill to the enemies
 * packed around the victim.
 */
public final class Lightning implements ChargedPerk {
	static final Lightning INSTANCE = new Lightning();

	/** Lightning fires after this many full attack cycles minus {@link #HIT_DISCOUNT} hits, never under {@link #MIN_HITS}. */
	private static final int CYCLES = 2;
	private static final int HIT_DISCOUNT = 2;
	private static final int MIN_HITS = 2;
	/** Damage in health points (2 per heart) for levels 1 to 3. */
	private static final float[] DAMAGE = {7.0F, 13.0F, 17.0F};
	private static final double BURST_RADIUS = 1.2;
	private static final float SHATTER_DAMAGE = 2.0F;
	private static final float SHATTER_SLOWED_DAMAGE = 1.15F;
	private static final double SHATTER_RADIUS = 2.5;
	/** Shatter kills: how far the chill spreads and to how many enemies. */
	private static final double SPREAD_RADIUS = 2.0;
	private static final int SPREAD_COUNT = 6;

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
	public int required(ItemStack weapon, int attacks) {
		return Math.max(MIN_HITS, CYCLES * Math.max(1, attacks) - HIT_DISCOUNT);
	}

	@Override
	public int keepTicks(int comboWindowTicks) {
		return BREAKS_WITH_COMBO;
	}

	@Override
	public int advance(int charge, SwingResult swing) {
		return charge + 1;
	}

	@Override
	public void fire(SwingResult swing, int level) {
		LivingEntity target = swing.primary();
		strike(swing.player(), swing.weapon(), target, damage(level));
		if (Synergy.CHARGED_LIGHTNING.on(swing.weapon())) {
			WindBurst.at(swing.player(), target.position(), BURST_RADIUS, 0.0F, target);
		}
	}

	/** Lightning's full damage at {@code level}. */
	static float damage(int level) {
		return DAMAGE[Math.clamp(level, 1, DAMAGE.length) - 1];
	}

	/** A visual bolt on {@code target} that deals {@code damage} as lightning from the player's {@code weapon}. */
	static void strike(ServerPlayer player, ItemStack weapon, LivingEntity target, float damage) {
		ServerLevel world = player.level();
		if (Synergy.SHATTER.on(weapon)) {
			if (target.isFullyFrozen()) {
				damage *= SHATTER_DAMAGE;
				shatter(player, target, Perks.level(weapon, Perks.COLD_EDGE));
			} else if (ColdEdge.chilled(target)) {
				damage *= SHATTER_SLOWED_DAMAGE;
			}
		}
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
		target.hurtServer(world, source, damage);
	}

	static void onKill(ServerPlayer player, ItemStack weapon, LivingEntity victim) {
		if (!Synergy.SHATTER.on(weapon)) {
			return;
		}
		int cold = Perks.level(weapon, Perks.COLD_EDGE);
		List<LivingEntity> near = Targets.enemiesNear(player, victim.position(), SPREAD_RADIUS, victim);
		for (LivingEntity target : near.subList(0, Math.min(near.size(), SPREAD_COUNT))) {
			ColdEdge.chill(target, cold);
		}
		ServerLevel world = player.level();
		world.sendParticles(ParticleTypes.SNOWFLAKE, victim.getX(), victim.getY(0.5), victim.getZ(), 20, 0.5, 0.4, 0.5, 0.1);
		world.playSound((Entity) null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.7F, 1.2F);
	}

	/** Shards of ice off a frozen target: a short chill on everything around it. */
	private static void shatter(ServerPlayer player, LivingEntity target, int cold) {
		ServerLevel world = player.level();
		for (LivingEntity near : Targets.enemiesNear(player, target.position(), SHATTER_RADIUS, target)) {
			ColdEdge.chill(near, cold);
		}
		world.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY(0.5), target.getZ(), 30, 0.4, 0.5, 0.4, 0.15);
		world.playSound((Entity) null, target.getX(), target.getY(), target.getZ(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0F, 0.8F);
	}
}
