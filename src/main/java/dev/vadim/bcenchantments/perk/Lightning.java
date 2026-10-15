package dev.vadim.bcenchantments.perk;

import dev.vadim.bcenchantments.Particles;
import dev.vadim.bcenchantments.combat.CombatHooks;
import dev.vadim.bcenchantments.combat.SwingResult;
import dev.vadim.bcenchantments.mob.Adaptation;
import java.util.ArrayList;
import java.util.Iterator;
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
import net.minecraft.world.phys.Vec3;

/**
 * Calls lightning down on the target when a Lightning combo completes: two full attack cycles, a
 * little shortened. With {@link Synergy#CHARGED_LIGHTNING} it bursts like a wind charge where it strikes.
 * With {@link Synergy#SHATTER}, any of the weapon's lightning that hits an enemy already frozen solid
 * (taking freeze damage) deals double damage and splashes a short chill around it; one that is only
 * slowed by Cold Edge takes 15% more. A kill with a Shatter weapon spreads its chill to the enemies
 * packed around the victim. {@link Synergy#LIGHTNING_ROD} pulls the crowd to the target before the bolt
 * lands, {@link Synergy#STORM_FRENZY} charges twice as fast while rampaging, {@link Synergy#SKYBREAKER}
 * meets launched targets at the top, and with {@link Synergy#TOXIC_STORM} or {@link Synergy#BLACK_LIGHTNING}
 * every bolt of the weapon leaves a poison splash or a soul burst where it strikes.
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
	/** Lightning Rod: how far the pull before the bolt reaches, and how long the bolt waits for it. */
	private static final double ROD_RADIUS = 5.0;
	private static final int ROD_DELAY = 6;
	/** How far from a target that died before its bolt the bolt looks for another enemy. */
	private static final double REDIRECT_RADIUS = 1.5;
	/** Lightning Rod's pull, as a multiple of Gravity's full pull. */
	private static final double ROD_PULL = 1.5;
	/** Skybreaker: share of Lightning's damage the bolt deals, and the longest it waits for the top. */
	private static final float SKYBREAKER = 0.5F;
	private static final int APEX_MAX_TICKS = 30;

	/** Skybreaker: a launched target on its way up, until the bolt meets it at the top. */
	private record Rising(ServerPlayer player, ItemStack weapon, LivingEntity target, int since) {
	}

	private static final List<Rising> RISING = new ArrayList<>();

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
		boolean frenzy = Synergy.STORM_FRENZY.on(swing.weapon()) && Rampaging.active(swing.player());
		return charge + (frenzy ? 2 : 1);
	}

	@Override
	public void fire(SwingResult swing, int level) {
		ServerPlayer player = swing.player();
		ItemStack weapon = swing.weapon();
		LivingEntity target = swing.primary();
		if (Synergy.LIGHTNING_ROD.on(weapon)) {
			// Lightning Rod: the crowd is drawn to the target, and the bolt waits for it to arrive.
			Vec3 rod = target.position();
			Gravity.pull(player, rod, ROD_RADIUS, Gravity.strength(Perks.level(weapon, Perks.GRAVITY)) * ROD_PULL, e -> e == target, true);
			CombatHooks.later(ROD_DELAY, () -> {
				if (player.isRemoved() || target.level() != player.level()) {
					return;
				}
				// A target that died meanwhile hands the bolt to the nearest enemy drawn to where it stood.
				LivingEntity struck = target;
				if (!target.isAlive()) {
					List<LivingEntity> near = Targets.enemiesNear(player, rod, ROD_RADIUS, target);
					struck = near.isEmpty() ? target : near.getFirst();
				}
				bolt(player, weapon, struck, level);
			});
			return;
		}
		bolt(player, weapon, target, level);
	}

	private static void bolt(ServerPlayer player, ItemStack weapon, LivingEntity target, int level) {
		strike(player, weapon, target, damage(level));
		if (Synergy.CHARGED_LIGHTNING.on(weapon)) {
			WindBurst.at(player, target.position(), BURST_RADIUS, 0.0F, target);
		}
	}

	/** A bolt that only looks the part, at {@code at}, for a strike with nothing left to hit. */
	private static void visualBolt(ServerPlayer player, Vec3 at) {
		LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(player.level(), EntitySpawnReason.TRIGGERED);
		if (bolt != null) {
			bolt.snapTo(at);
			bolt.setVisualOnly(true);
			bolt.setCause(player);
			player.level().addFreshEntity(bolt);
		}
	}

	/** Skybreaker: {@code target} was just launched; a half-power bolt meets it at the top of its flight. */
	static void skybreaker(ServerPlayer player, ItemStack weapon, LivingEntity target) {
		if (Synergy.SKYBREAKER.on(weapon)) {
			RISING.add(new Rising(player, weapon.copy(), target, player.tickCount));
		}
	}

	static void tick() {
		Iterator<Rising> it = RISING.iterator();
		while (it.hasNext()) {
			Rising rising = it.next();
			LivingEntity target = rising.target();
			ServerPlayer player = rising.player();
			int age = player.tickCount - rising.since();
			if (player.isRemoved() || target.level() != player.level() || age > APEX_MAX_TICKS || age < 0) {
				it.remove();
				continue;
			}
			// The launch's speed is applied a tick after it is set, so wait until the target has risen and turned.
			// A target that dies on the way up gets its bolt where it died.
			if (!target.isAlive() || age >= 2 && target.getDeltaMovement().y <= 0.0) {
				strike(player, rising.weapon(), target, damage(Perks.level(rising.weapon(), Perks.LIGHTNING)) * SKYBREAKER);
				it.remove();
			}
		}
	}

	/** Lightning's full damage at {@code level}. */
	static float damage(int level) {
		return DAMAGE[Math.clamp(level, 1, DAMAGE.length) - 1];
	}

	/**
	 * A visual bolt on {@code target} that deals {@code damage} as lightning from the player's {@code weapon}.
	 * A target that died before the bolt arrived hands it to the nearest enemy within reach of where it
	 * stood; with none there the bolt still strikes the spot, and its splash and soul burst still go off.
	 */
	static void strike(ServerPlayer player, ItemStack weapon, LivingEntity target, float damage) {
		ServerLevel world = player.level();
		if (!target.isAlive()) {
			Vec3 spot = target.position();
			List<LivingEntity> near = target.level() == world ? Targets.enemiesNear(player, spot, REDIRECT_RADIUS, target) : List.of();
			if (near.isEmpty()) {
				visualBolt(player, spot);
				spotEffects(player, weapon, spot, spot.add(0.0, target.getBbHeight() * 0.5, 0.0));
				return;
			}
			target = near.getFirst();
		}
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
		Vec3 at = target.position();
		Vec3 center = at.add(0.0, target.getBbHeight() * 0.5, 0.0);
		// The finishing slash just hit, so drop the hurt cooldown or most of the bolt would be swallowed.
		target.invulnerableTime = 0;
		target.hurtServer(world, source, Adaptation.damage(target, Perks.LIGHTNING, damage));
		spotEffects(player, weapon, at, center);
	}

	/** What a bolt leaves where it strikes: Toxic Storm's splash and Black Lightning's soul burst. */
	private static void spotEffects(ServerPlayer player, ItemStack weapon, Vec3 at, Vec3 center) {
		if (Synergy.TOXIC_STORM.on(weapon)) {
			Venomous.splash(player, at, Perks.level(weapon, Perks.VENOMOUS));
		}
		if (Synergy.BLACK_LIGHTNING.on(weapon)) {
			Withering.burst(player, center, Perks.level(weapon, Perks.WITHERING));
		}
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
		Particles.send(world, ParticleTypes.SNOWFLAKE, victim.getX(), victim.getY(0.5), victim.getZ(), 20, 0.5, 0.4, 0.5, 0.1);
		world.playSound((Entity) null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.7F, 1.2F);
	}

	/** Shards of ice off a frozen target: a short chill on everything around it. */
	private static void shatter(ServerPlayer player, LivingEntity target, int cold) {
		ServerLevel world = player.level();
		for (LivingEntity near : Targets.enemiesNear(player, target.position(), SHATTER_RADIUS, target)) {
			ColdEdge.chill(near, cold);
		}
		Particles.send(world, ParticleTypes.SNOWFLAKE, target.getX(), target.getY(0.5), target.getZ(), 30, 0.4, 0.5, 0.4, 0.15);
		world.playSound((Entity) null, target.getX(), target.getY(), target.getZ(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0F, 0.8F);
	}
}
