package dev.vadim.bcenchantments.perk;

import dev.vadim.bcenchantments.Particles;
import dev.vadim.bcenchantments.TrimEnchantments;
import dev.vadim.bcenchantments.mixin.MobAccessor;
import dev.vadim.bcenchantments.mob.Adaptation;
import dev.vadim.bcenchantments.progression.Refinement;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.item.ItemStack;

/**
 * Three kills within 10 s summon a vex that fights for the player: it hunts the monsters around them
 * and vanishes after a few hits of its own. It ignores players, and nothing the player does hurts it.
 * With {@link Synergy#STATIC_GUARD} each vex hit strikes its target with a quarter-power lightning bolt.
 */
public final class GuardianStrike {
	/** The tag every summoned vex carries, so monster scaling and the player's own powers leave it alone. */
	public static final String TAG = TrimEnchantments.MOD_ID + ".guardian";
	private static final int KILLS = 3;
	private static final int WINDOW_TICKS = 200;
	/** Hits a vex lands before it vanishes, for levels 1 to 3. */
	private static final int[] HITS = {2, 3, 4};
	/** A vex vanishes on its own after 30 s, and comes back to the player from further than this. */
	private static final int LIFE_TICKS = 600;
	private static final double LEASH = 20.0;
	private static final double HUNT_RANGE = 12.0;
	private static final float STATIC_GUARD = 0.25F;

	private static final class Guard {
		final ServerPlayer owner;
		final ItemStack weapon;
		int hitsLeft;

		Guard(ServerPlayer owner, ItemStack weapon, int hits) {
			this.owner = owner;
			this.weapon = weapon;
			this.hitsLeft = hits;
		}
	}

	private static final Map<Vex, Guard> GUARDS = new WeakHashMap<>();
	private static final Map<ServerPlayer, Deque<Integer>> KILL_TICKS = new WeakHashMap<>();

	private GuardianStrike() {
	}

	static void onKill(ServerPlayer player, ItemStack weapon, LivingEntity victim) {
		int level = Math.min(Perks.level(weapon, Perks.GUARDIAN_STRIKE), HITS.length);
		if (level <= 0 || !(victim instanceof Enemy) || guardian(victim)) {
			return;
		}
		Deque<Integer> kills = KILL_TICKS.computeIfAbsent(player, p -> new ArrayDeque<>());
		kills.addLast(player.tickCount);
		while (!kills.isEmpty() && (player.tickCount - kills.peekFirst() > WINDOW_TICKS || kills.peekFirst() > player.tickCount)) {
			kills.removeFirst();
		}
		if (kills.size() >= KILLS) {
			kills.clear();
			summon(player, weapon, level);
		}
	}

	/** Summons a vex at the player's shoulder that fights for them for {@code level}'s number of hits. */
	static void summon(ServerPlayer player, ItemStack weapon, int level) {
		ServerLevel world = player.level();
		Vex vex = EntityTypes.VEX.create(world, EntitySpawnReason.MOB_SUMMONED);
		if (vex == null) {
			return;
		}
		Adaptation.used(player, weapon, Perks.GUARDIAN_STRIKE);
		vex.snapTo(player.getX(), player.getY() + 1.6, player.getZ(), player.getYRot(), 0.0F);
		vex.addTag(TAG);
		vex.setLimitedLife(LIFE_TICKS);
		((MobAccessor) vex).bcenchantments$targetSelector().removeAllGoals(goal -> true);
		GUARDS.put(vex, new Guard(player, weapon.copy(), HITS[Math.clamp(level, 1, HITS.length) - 1]));
		world.addFreshEntity(vex);
		Particles.send(world, ParticleTypes.ENCHANT, vex.getX(), vex.getY(0.5), vex.getZ(), 20, 0.3, 0.3, 0.3, 0.5);
		world.playSound((Entity) null, vex.getX(), vex.getY(), vex.getZ(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.PLAYERS, 0.6F, 1.4F);
	}

	/** Whether {@code entity} is a vex summoned by this power. */
	public static boolean guardian(Entity entity) {
		return entity instanceof Vex && entity.entityTags().contains(TAG);
	}

	/** Nothing a player does hurts a summoned vex. */
	static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
		return !(guardian(entity) && source.getEntity() instanceof ServerPlayer);
	}

	/** A vex's hit: one less left, and Static Guard's bolt. */
	static void afterDamage(LivingEntity target, DamageSource source, float baseDamage, float damage, boolean blocked) {
		if (!(source.getEntity() instanceof Vex vex)) {
			return;
		}
		Guard guard = GUARDS.get(vex);
		if (guard == null || blocked) {
			return;
		}
		if (Synergy.STATIC_GUARD.on(guard.weapon) && !guard.owner.isRemoved()) {
			Lightning.strike(guard.owner, guard.weapon, target, Lightning.damage(Perks.level(guard.weapon, Perks.LIGHTNING)) * STATIC_GUARD);
		}
		if (--guard.hitsLeft <= 0) {
			vanish(vex);
		}
	}

	static void tick() {
		Iterator<Map.Entry<Vex, Guard>> it = GUARDS.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Vex, Guard> entry = it.next();
			Vex vex = entry.getKey();
			Guard guard = entry.getValue();
			if (vex.isRemoved() || !vex.isAlive()) {
				it.remove();
				continue;
			}
			ServerPlayer owner = guard.owner;
			if (owner.isRemoved() || owner.level() != vex.level()) {
				vanish(vex);
				it.remove();
				continue;
			}
			if (vex.distanceToSqr(owner) > LEASH * LEASH) {
				vex.teleportTo(owner.getX(), owner.getY() + 1.6, owner.getZ());
			}
			LivingEntity target = vex.getTarget();
			if (target == null || !huntable(owner, target)) {
				LivingEntity next = null;
				for (LivingEntity enemy : Targets.enemiesNear(owner, owner.position(), HUNT_RANGE, null)) {
					if (huntable(owner, enemy)) {
						next = enemy;
						break;
					}
				}
				vex.setTarget(next);
			}
		}
	}

	/** A monster near {@code owner} a vex may hunt: not one that counters Guardian Strike. */
	private static boolean huntable(ServerPlayer owner, LivingEntity target) {
		return target.isAlive() && target instanceof Enemy && Targets.harmable(owner, target)
			&& target.distanceToSqr(owner) <= HUNT_RANGE * HUNT_RANGE
			&& Adaptation.counter(target) != Refinement.Perk.GUARDIAN_STRIKE;
	}

	private static void vanish(Vex vex) {
		if (vex.level() instanceof ServerLevel world) {
			Particles.send(world, ParticleTypes.POOF, vex.getX(), vex.getY(0.5), vex.getZ(), 10, 0.2, 0.2, 0.2, 0.02);
			world.playSound((Entity) null, vex.getX(), vex.getY(), vex.getZ(), SoundEvents.VEX_DEATH, SoundSource.PLAYERS, 0.6F, 1.3F);
		}
		vex.discard();
	}
}
