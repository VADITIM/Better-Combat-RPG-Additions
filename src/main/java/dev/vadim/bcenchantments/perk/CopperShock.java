package dev.vadim.bcenchantments.perk;

import dev.vadim.bcenchantments.Particles;
import dev.vadim.bcenchantments.combat.CombatHooks;
import dev.vadim.bcenchantments.combat.SwingResult;
import dev.vadim.bcenchantments.mob.Adaptation;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * A hit has a chance to release a copper-coloured pulse from its target that jumps from enemy to enemy,
 * a little damage at each, never to the same enemy twice. With {@link Synergy#COPPER_STORM} every enemy
 * it reaches has an even chance to be struck by a quarter-power lightning bolt. With
 * {@link Synergy#SPARK_BURST} every enemy a death burst pushes zaps one more ({@link #zap}).
 */
final class CopperShock {
	/** Chance a swing releases a pulse, and how many enemies it reaches, for levels 1 to 3. */
	private static final float[] CHANCE = {0.10F, 0.15F, 0.20F};
	private static final int[] JUMPS = {4, 5, 6};
	private static final float DAMAGE = 3.0F;
	private static final double JUMP_RANGE = 4.0;
	private static final int JUMP_TICKS = 2;
	private static final float COPPER_STORM_CHANCE = 0.5F;
	private static final float COPPER_STORM = 0.25F;
	private static final DustParticleOptions COPPER = new DustParticleOptions(0xD9783F, 1.0F);

	private CopperShock() {
	}

	static void onSwing(SwingResult swing) {
		roll(swing.player(), swing.weapon(), swing.primary());
	}

	static void onDirectHit(ServerPlayer player, ItemStack weapon, LivingEntity target) {
		roll(player, weapon, target);
	}

	private static void roll(ServerPlayer player, ItemStack weapon, LivingEntity target) {
		int level = Math.min(Perks.level(weapon, Perks.COPPER_SHOCK), CHANCE.length);
		if (level > 0 && player.getRandom().nextFloat() < CHANCE[level - 1]) {
			Adaptation.used(player, weapon, Perks.COPPER_SHOCK);
			pulse(player, weapon, target, level);
		}
	}

	/** Releases a pulse that starts at {@code from} and jumps through {@code level}'s number of enemies. */
	static void pulse(ServerPlayer player, ItemStack weapon, LivingEntity from, int level) {
		Set<LivingEntity> reached = new HashSet<>();
		jump(player, weapon.copy(), from, JUMPS[Math.clamp(level, 1, JUMPS.length) - 1], reached);
	}

	private static void jump(ServerPlayer player, ItemStack weapon, LivingEntity at, int left, Set<LivingEntity> reached) {
		if (left <= 0 || player.isRemoved() || at.level() != player.level()) {
			return;
		}
		ServerLevel world = player.level();
		reached.add(at);
		if (at.isAlive()) {
			at.invulnerableTime = 0;
			at.hurtServer(world, world.damageSources().indirectMagic(player, player), Adaptation.damage(at, Perks.COPPER_SHOCK, DAMAGE));
			Particles.send(world, ParticleTypes.ELECTRIC_SPARK, at.getX(), at.getY(0.5), at.getZ(), 10, 0.3, 0.4, 0.3, 0.1);
			Particles.send(world, ParticleTypes.WAX_OFF, at.getX(), at.getY(0.5), at.getZ(), 4, 0.3, 0.4, 0.3, 0.5);
			world.playSound((Entity) null, at.getX(), at.getY(), at.getZ(), SoundEvents.COPPER_BULB_TURN_ON, SoundSource.PLAYERS, 0.8F, 1.6F);
			if (Synergy.COPPER_STORM.on(weapon) && player.getRandom().nextFloat() < COPPER_STORM_CHANCE) {
				Lightning.strike(player, weapon, at, Lightning.damage(Perks.level(weapon, Perks.LIGHTNING)) * COPPER_STORM);
			}
		}
		Vec3 from = at.position().add(0.0, at.getBbHeight() * 0.5, 0.0);
		LivingEntity next = null;
		for (LivingEntity enemy : Targets.enemiesNear(player, at.position(), JUMP_RANGE, at)) {
			if (!reached.contains(enemy)) {
				next = enemy;
				break;
			}
		}
		if (next == null) {
			return;
		}
		arc(world, from, next.position().add(0.0, next.getBbHeight() * 0.5, 0.0));
		LivingEntity target = next;
		CombatHooks.later(JUMP_TICKS, () -> jump(player, weapon, target, left - 1, reached));
	}

	/**
	 * Spark Burst: one jump of a pulse from {@code from} to the nearest other enemy within a jump's
	 * reach, which takes a pulse's damage.
	 */
	static void zap(ServerPlayer player, ItemStack weapon, LivingEntity from) {
		if (player.isRemoved() || from.level() != player.level()) {
			return;
		}
		List<LivingEntity> near = Targets.enemiesNear(player, from.position(), JUMP_RANGE, from);
		if (near.isEmpty()) {
			return;
		}
		ServerLevel world = player.level();
		LivingEntity target = near.getFirst();
		arc(world, from.position().add(0.0, from.getBbHeight() * 0.5, 0.0), target.position().add(0.0, target.getBbHeight() * 0.5, 0.0));
		target.invulnerableTime = 0;
		target.hurtServer(world, world.damageSources().indirectMagic(player, player), Adaptation.damage(target, Perks.COPPER_SHOCK, DAMAGE));
		Particles.send(world, ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY(0.5), target.getZ(), 6, 0.3, 0.4, 0.3, 0.1);
		world.playSound((Entity) null, target.getX(), target.getY(), target.getZ(), SoundEvents.COPPER_BULB_TURN_ON, SoundSource.PLAYERS, 0.6F, 1.8F);
	}

	/** A copper line from {@code from} to {@code to}, jagged like a spark. */
	private static void arc(ServerLevel world, Vec3 from, Vec3 to) {
		int steps = Math.max(2, (int) (from.distanceTo(to) / 0.3));
		for (int i = 0; i <= steps; i++) {
			Vec3 p = from.lerp(to, i / (double) steps);
			double jitter = i == 0 || i == steps ? 0.0 : 0.12;
			Particles.send(world, COPPER, p.x, p.y, p.z, 1, jitter, jitter, jitter, 0.0);
		}
	}
}
