package dev.vadim.bcenchantments.perk;

import dev.vadim.bcenchantments.Particles;
import dev.vadim.bcenchantments.combat.CombatHooks;
import dev.vadim.bcenchantments.mob.Adaptation;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * A kill's overkill, the damage beyond the victim's last health, goes to the nearest other monsters
 * within 3 blocks as magic damage credited to the player, each taking all of it. It goes no further: a
 * monster that overkill kills passes nothing on. With {@link Synergy#ARC} the overkill of a kill by the
 * weapon's lightning arrives as lightning bolts; with {@link Synergy#SHRAPNEL} every monster it reaches
 * bursts like a wind charge for 4 damage.
 */
final class SharedPain {
	private static final double RADIUS = 3.0;
	/** How many monsters take the overkill, for levels 1 to 3. */
	private static final int[] COUNT = {1, 2, 3};
	/** Overkill below this is not worth carrying. */
	private static final float MIN = 0.5F;
	/** Shrapnel: the wind burst at each monster the overkill reaches. */
	private static final double SHRAPNEL_RADIUS = 2.0;
	private static final float SHRAPNEL_DAMAGE = 4.0F;

	/** Health a victim had before the hit, then the overkill of the hit that killed it. */
	private record Hurt(float before, float overkill) {
	}

	private static final Map<LivingEntity, Hurt> HURT = new WeakHashMap<>();
	/** Monsters overkill is being dealt to right now, so a kill by it does not spread again. */
	private static final Map<LivingEntity, Boolean> RECEIVING = new WeakHashMap<>();
	/** Whether a Shrapnel burst is going off right now, whose kills pass nothing on. */
	private static boolean shrapnelBursting;

	private SharedPain() {
	}

	/** Before a hit lands: notes the health it lands on. */
	static boolean beforeDamage(LivingEntity target, DamageSource source, float amount) {
		if (target.isAlive() && target instanceof Enemy) {
			HURT.put(target, new Hurt(target.getHealth(), 0.0F));
		}
		return true;
	}

	/** After a hit landed: what it did beyond the health that was there. */
	static void afterDamage(LivingEntity target, DamageSource source, float baseDamage, float damage, boolean blocked) {
		Hurt hurt = HURT.get(target);
		if (hurt != null && !blocked && damage > hurt.before) {
			HURT.put(target, new Hurt(hurt.before, damage - hurt.before));
		}
	}

	static void onKill(ServerPlayer player, ItemStack weapon, LivingEntity victim) {
		int level = Math.min(Perks.level(weapon, Perks.SHARED_PAIN), COUNT.length);
		if (level <= 0 || !(victim instanceof Enemy) || RECEIVING.containsKey(victim) || shrapnelBursting) {
			return;
		}
		Vec3 from = victim.position().add(0.0, victim.getBbHeight() * 0.5, 0.0);
		DamageSource blow = CombatHooks.killingBlow();
		boolean arc = Synergy.ARC.on(weapon) && blow != null && blow.is(DamageTypeTags.IS_LIGHTNING);
		boolean shrapnel = Synergy.SHRAPNEL.on(weapon);
		// A tick later, so the hit's overkill has been noted, and the victim is gone from the crowd.
		CombatHooks.later(1, () -> {
			Hurt hurt = HURT.remove(victim);
			if (hurt == null || hurt.overkill < MIN || player.isRemoved() || player.level() != victim.level()) {
				return;
			}
			ServerLevel world = player.level();
			List<LivingEntity> near = Targets.enemiesNear(player, from, RADIUS, victim);
			if (near.isEmpty()) {
				return;
			}
			Adaptation.used(player, weapon, Perks.SHARED_PAIN);
			for (LivingEntity next : near.subList(0, Math.min(near.size(), COUNT[level - 1]))) {
				RECEIVING.put(next, true);
				try {
					if (arc) {
						Lightning.strike(player, weapon, next, Adaptation.damage(next, Perks.SHARED_PAIN, hurt.overkill));
					} else {
						next.invulnerableTime = 0;
						next.hurtServer(world, world.damageSources().indirectMagic(player, player),
							Adaptation.damage(next, Perks.SHARED_PAIN, hurt.overkill));
					}
					if (shrapnel) {
						// A kill by the burst passes nothing on either, so it cannot run through a crowd.
						shrapnelBursting = true;
						try {
							WindBurst.at(player, next.position().add(0.0, next.getBbHeight() * 0.5, 0.0), SHRAPNEL_RADIUS, SHRAPNEL_DAMAGE, null);
						} finally {
							shrapnelBursting = false;
						}
					}
				} finally {
					RECEIVING.remove(next);
				}
				trail(world, from, next.position().add(0.0, next.getBbHeight() * 0.5, 0.0));
				world.playSound((Entity) null, next.getX(), next.getY(), next.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.5F, 0.8F);
			}
		});
	}

	/** A line of red sparks from the dead monster to one that takes its overkill. */
	private static void trail(ServerLevel world, Vec3 from, Vec3 to) {
		Vec3 step = to.subtract(from);
		int steps = Math.max(2, (int) (step.length() * 2.0));
		for (int i = 0; i <= steps; i++) {
			Vec3 at = from.add(step.scale(i / (double) steps));
			Particles.send(world, ParticleTypes.DAMAGE_INDICATOR, at.x, at.y, at.z, 1, 0.0, 0.0, 0.0, 0.0);
		}
	}
}
