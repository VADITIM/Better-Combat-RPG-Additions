package dev.vadim.bcenchantments.perk;

import dev.vadim.bcenchantments.Particles;
import dev.vadim.bcenchantments.combat.ComboTracker;
import dev.vadim.bcenchantments.combat.SwingResult;
import dev.vadim.bcenchantments.mob.Adaptation;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * A point-blank hit marks its target; the next point-blank hit on it spends the mark for extra damage,
 * so the player keeps closing in instead of poking from the edge of their reach. The extra damage lands
 * as a second hit right after the swing. Synergies:
 * <ul>
 * <li>{@link Synergy#LAUNCH}: a launching swing on a marked mob throws its targets higher, and the
 * player goes up after them.</li>
 * <li>{@link Synergy#BLAST_STEP}: a kill by a point-blank hit on a marked mob bursts the player onward.</li>
 * <li>{@link Synergy#THUNDER_CHARGE}: spending a mark adds Lightning charge.</li>
 * </ul>
 */
public final class Marked {
	/** How close, centre to centre, a hit has to land to count as point blank. */
	private static final double POINT_BLANK = 2.5;
	/** How long a mark lasts: 5 s. */
	private static final int MARK_TICKS = 100;
	/** Damage of a hit that spends a mark, as a multiple of the swing, for levels 1 to 3. */
	private static final float[] MULTIPLIER = {1.5F, 2.0F, 2.5F};
	/** Launch: how much higher the targets go, and how much of their speed the player follows with. */
	private static final double LAUNCH_BOOST = 1.3;
	private static final double LAUNCH_FOLLOW = 0.9;
	/** Blast Step: the player's speed onward and up. */
	private static final double BLAST_SPEED = 1.0;
	private static final double BLAST_LIFT = 0.3;
	/** Thunder Charge: Lightning charge a spent mark adds, in hits. */
	private static final int THUNDER_CHARGE = 3;
	private static final DustParticleOptions MARK = new DustParticleOptions(0xC8A07A, 1.2F);

	private static final class Mark {
		final ServerPlayer owner;
		long until;

		Mark(ServerPlayer owner) {
			this.owner = owner;
		}
	}

	private static final Map<LivingEntity, Mark> MARKS = new WeakHashMap<>();
	/** Targets whose mark was spent, by the tick it was, so a kill by that hit still counts as one on a marked mob. */
	private static final Map<LivingEntity, Long> SPENT = new WeakHashMap<>();
	/** Players by the tick of their last Blast Step, so one swing's kills burst the player once. */
	private static final Map<ServerPlayer, Long> BLASTED = new WeakHashMap<>();
	private static long now;

	private Marked() {
	}

	static void onSwing(SwingResult swing) {
		ServerPlayer player = swing.player();
		float damage = (float) (player.getAttributeValue(Attributes.ATTACK_DAMAGE) * swing.hand().attack().damageMultiplier());
		if (spend(player, swing.weapon(), swing.primary(), damage) && Synergy.THUNDER_CHARGE.on(swing.weapon())) {
			ComboTracker.addCharge(swing, Lightning.INSTANCE, THUNDER_CHARGE);
		}
	}

	static void onDirectHit(ServerPlayer player, ItemStack weapon, LivingEntity target) {
		spend(player, weapon, target, (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE));
	}

	/** Marks {@code target}, or spends its mark for extra damage; true if a mark was spent. */
	private static boolean spend(ServerPlayer player, ItemStack weapon, LivingEntity target, float damage) {
		int level = Math.min(Perks.level(weapon, Perks.MARKED), MULTIPLIER.length);
		if (level <= 0 || !target.isAlive() || !pointBlank(player, target)) {
			return false;
		}
		ServerLevel world = player.level();
		if (!markedBy(target, player)) {
			if (Adaptation.resists(target, Perks.MARKED)) {
				return false;
			}
			Mark mark = new Mark(player);
			mark.until = now + MARK_TICKS;
			MARKS.put(target, mark);
			Particles.send(world, MARK, target.getX(), target.getY(0.6), target.getZ(), 10, target.getBbWidth() * 0.4, target.getBbHeight() * 0.3, target.getBbWidth() * 0.4, 0.0);
			world.playSound((Entity) null, target.getX(), target.getY(), target.getZ(), SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.PLAYERS, 0.7F, 1.6F);
			return false;
		}
		MARKS.remove(target);
		SPENT.put(target, now);
		Adaptation.used(player, weapon, Perks.MARKED);
		target.invulnerableTime = 0;
		target.hurtServer(world, world.damageSources().playerAttack(player), Adaptation.damage(target, Perks.MARKED, damage * (MULTIPLIER[level - 1] - 1.0F)));
		Particles.send(world, ParticleTypes.CRIT, target.getX(), target.getY(0.5), target.getZ(), 14, 0.3, 0.4, 0.3, 0.35);
		Particles.send(world, MARK, target.getX(), target.getY(0.5), target.getZ(), 16, 0.4, 0.4, 0.4, 0.0);
		world.playSound((Entity) null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 0.7F);
		return true;
	}

	/** Whether {@code target} stands close enough to {@code player} for a point-blank hit. */
	static boolean pointBlank(ServerPlayer player, LivingEntity target) {
		return player.distanceToSqr(target) <= POINT_BLANK * POINT_BLANK;
	}

	/** Whether {@code target} carries a mark {@code player} put on it. */
	static boolean markedBy(LivingEntity target, ServerPlayer player) {
		Mark mark = MARKS.get(target);
		return mark != null && mark.owner == player && now <= mark.until;
	}

	/** Whether {@code target} is marked by {@code player} or had its mark spent this tick. */
	private static boolean hitWhileMarked(LivingEntity target, ServerPlayer player) {
		Long spent = SPENT.get(target);
		return markedBy(target, player) || spent != null && spent == now;
	}

	/**
	 * Launch: the launch speed for a swing by {@code player} that struck {@code target}, boosted if it is
	 * marked; with {@code playerFollows} the player goes up after the targets.
	 */
	static double launch(ServerPlayer player, ItemStack weapon, LivingEntity target, double speed, boolean playerFollows) {
		if (!Synergy.LAUNCH.on(weapon) || !markedBy(target, player)) {
			return speed;
		}
		double boosted = speed * LAUNCH_BOOST;
		if (playerFollows) {
			Vec3 motion = player.getDeltaMovement();
			player.setDeltaMovement(motion.x * 0.3, boosted * LAUNCH_FOLLOW, motion.z * 0.3);
			player.hurtMarked = true;
			Particles.send(player.level(), ParticleTypes.GUST_EMITTER_SMALL, player.getX(), player.getY(), player.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
		}
		return boosted;
	}

	/** Blast Step: a kill by a point-blank hit on a marked mob bursts the player onward, once per swing. */
	static void onKill(ServerPlayer player, ItemStack weapon, LivingEntity victim) {
		if (ComboTracker.currentSwing(player) == null || !Synergy.BLAST_STEP.on(weapon)
			|| !hitWhileMarked(victim, player) || !pointBlank(player, victim)) {
			return;
		}
		Long last = BLASTED.get(player);
		if (last != null && last == now) {
			return;
		}
		BLASTED.put(player, now);
		Vec3 dir = facing(player);
		player.setDeltaMovement(dir.x * BLAST_SPEED, BLAST_LIFT, dir.z * BLAST_SPEED);
		player.hurtMarked = true;
		player.resetFallDistance();
		ServerLevel world = player.level();
		Particles.send(world, ParticleTypes.GUST_EMITTER_SMALL, player.getX(), player.getY() + 0.5, player.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
		world.playSound((Entity) null, player.getX(), player.getY(), player.getZ(), SoundEvents.WIND_CHARGE_BURST, SoundSource.PLAYERS, 0.8F, 1.2F);
	}

	static void tick() {
		now++;
		Iterator<Map.Entry<LivingEntity, Mark>> it = MARKS.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<LivingEntity, Mark> entry = it.next();
			LivingEntity target = entry.getKey();
			if (target.isRemoved() || !target.isAlive() || now > entry.getValue().until) {
				it.remove();
				continue;
			}
			if (now % 5 == 0 && target.level() instanceof ServerLevel world) {
				Particles.send(world, MARK, target.getX(), target.getY() + target.getBbHeight() + 0.3, target.getZ(), 1, 0.1, 0.05, 0.1, 0.0);
			}
		}
		SPENT.values().removeIf(tick -> tick < now);
	}

	/** The way the player looks, flat; the way the body faces when looking straight up or down. */
	private static Vec3 facing(ServerPlayer player) {
		Vec3 look = player.getLookAngle();
		Vec3 dir = new Vec3(look.x, 0.0, look.z);
		if (dir.lengthSqr() < 1.0E-4) {
			double yaw = Math.toRadians(player.getYRot());
			dir = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
		}
		return dir.normalize();
	}
}
