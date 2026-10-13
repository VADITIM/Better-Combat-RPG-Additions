package dev.vadim.bcenchantments.perk;

import dev.vadim.bcenchantments.Particles;
import dev.vadim.bcenchantments.combat.ComboTracker;
import dev.vadim.bcenchantments.combat.SwingResult;
import dev.vadim.bcenchantments.weapon.WeaponStyles;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * A run-up empowers the next swing: after sprinting for a moment, the first swing that lands within
 * half a second throws its targets back hard. The run-up is spent by that swing, so the player has
 * to break away and charge in again. Synergies:
 * <ul>
 * <li>{@link Synergy#LAUNCH}: an empowered launching swing throws the targets higher and the player
 * up after them.</li>
 * <li>{@link Synergy#BLAST_STEP}: a kill by an empowered swing bursts the player onward.</li>
 * </ul>
 */
public final class Momentum {
	/** Sprinting this long builds a run-up, which lasts this long after the sprint ends. */
	private static final int RUN_UP_TICKS = 8;
	private static final int KEEP_TICKS = 10;
	/** Knockback speed of an empowered swing for levels 1 to 3, and its lift. */
	private static final double[] PUSH = {0.5, 0.7, 0.9};
	private static final double LIFT = 0.25;
	/** Launch: how much higher the targets go, and how much of their speed the player follows with. */
	private static final double LAUNCH_BOOST = 1.3;
	private static final double LAUNCH_FOLLOW = 0.9;
	/** Blast Step: the player's speed onward and up. */
	private static final double BLAST_SPEED = 1.0;
	private static final double BLAST_LIFT = 0.3;

	private static final class Run {
		int start = -1;
		int last = -1;
		/** Tick of the last Blast Step, so one swing's kills burst the player once. */
		int blasted = -1;
	}

	private static final Map<ServerPlayer, Run> RUNS = new WeakHashMap<>();

	private Momentum() {
	}

	static void tick(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (!player.isSprinting()) {
				continue;
			}
			Run run = RUNS.computeIfAbsent(player, p -> new Run());
			if (run.last < player.tickCount - 1) {
				run.start = player.tickCount;
			}
			run.last = player.tickCount;
		}
	}

	/** Whether {@code player} has a run-up for their next swing. */
	static boolean ready(ServerPlayer player) {
		Run run = RUNS.get(player);
		return run != null && run.start >= 0 && player.tickCount - run.last <= KEEP_TICKS && run.last - run.start >= RUN_UP_TICKS;
	}

	static void onSwing(SwingResult swing) {
		ServerPlayer player = swing.player();
		if (!ready(player)) {
			return;
		}
		int level = Math.min(Perks.level(swing.weapon(), Perks.MOMENTUM), PUSH.length);
		if (level <= 0) {
			return;
		}
		// Spent by the first swing that lands, whatever it did.
		RUNS.get(player).start = -1;
		ServerLevel world = player.level();
		world.playSound((Entity) null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.0F, 0.7F);
		// Launch throws the targets straight up after the player; pushing them away as well would part them.
		if (Synergy.LAUNCH.on(swing.weapon()) && WeaponStyles.launches(player, swing.hand())) {
			return;
		}
		Vec3 dir = facing(player);
		for (LivingEntity target : swing.hits()) {
			Vec3 motion = target.getDeltaMovement();
			target.setDeltaMovement(motion.x * 0.3 + dir.x * PUSH[level - 1], Math.max(motion.y, LIFT), motion.z * 0.3 + dir.z * PUSH[level - 1]);
			target.hurtMarked = true;
			Particles.send(world, ParticleTypes.CLOUD, target.getX(), target.getY(0.3), target.getZ(), 6, 0.2, 0.2, 0.2, 0.05);
		}
	}

	/** Launch: the launch speed for a swing by {@code player}, boosted after a run-up. Sends the player up after the targets. */
	static double launch(ServerPlayer player, ItemStack weapon, double speed, boolean playerFollows) {
		if (!Synergy.LAUNCH.on(weapon) || !ready(player)) {
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

	/** Blast Step: a kill by an empowered swing bursts the player onward, once per swing. */
	static void onKill(ServerPlayer player, ItemStack weapon, LivingEntity victim) {
		if (ComboTracker.currentSwing(player) == null || !Synergy.BLAST_STEP.on(weapon) || !ready(player)) {
			return;
		}
		Run run = RUNS.get(player);
		if (run.blasted == player.tickCount) {
			return;
		}
		run.blasted = player.tickCount;
		Vec3 dir = facing(player);
		player.setDeltaMovement(dir.x * BLAST_SPEED, BLAST_LIFT, dir.z * BLAST_SPEED);
		player.hurtMarked = true;
		player.resetFallDistance();
		ServerLevel world = player.level();
		Particles.send(world, ParticleTypes.GUST_EMITTER_SMALL, player.getX(), player.getY() + 0.5, player.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
		world.playSound((Entity) null, player.getX(), player.getY(), player.getZ(), SoundEvents.WIND_CHARGE_BURST, SoundSource.PLAYERS, 0.8F, 1.2F);
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
