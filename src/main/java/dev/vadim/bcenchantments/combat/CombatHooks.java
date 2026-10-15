package dev.vadim.bcenchantments.combat;

import java.util.ArrayList;
import java.util.List;
import net.bettercombat.api.AttackHand;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Where features plug into combat on the server. A new perk or weapon mechanic registers here from
 * its own {@code init} instead of being wired into {@link ComboTracker} by hand.
 */
public final class CombatHooks {
	/** Runs once after every Better Combat swing that landed at least one hit. */
	@FunctionalInterface
	public interface SwingEffect {
		void afterSwing(SwingResult swing);
	}

	/** Runs after a plain vanilla attack that landed outside any Better Combat swing. */
	@FunctionalInterface
	public interface DirectHitEffect {
		void onDirectHit(ServerPlayer player, ItemStack weapon, LivingEntity target);
	}

	/**
	 * Runs when a player kills a monster or animal, by any means credited to them: a hit, a bolt, a
	 * burst. {@code weapon} is the item of the swing under way, or else the main hand.
	 */
	@FunctionalInterface
	public interface KillEffect {
		void onKill(ServerPlayer player, ItemStack weapon, LivingEntity victim);
	}

	private static final List<SwingEffect> SWING = new ArrayList<>();
	private static final List<KillEffect> KILL = new ArrayList<>();
	private static final List<DirectHitEffect> DIRECT_HIT = new ArrayList<>();
	private static final List<Runnable> SERVER_TICK = new ArrayList<>();

	private record Scheduled(long due, Runnable task) {
	}

	private static final List<Scheduled> LATER = new ArrayList<>();
	private static long now;
	private static @Nullable DamageSource killingBlow;

	private CombatHooks() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			now++;
			runDue();
			SERVER_TICK.forEach(Runnable::run);
		});
		ServerLivingEntityEvents.AFTER_DEATH.register(CombatHooks::kill);
	}

	public static void onSwing(SwingEffect effect) {
		SWING.add(effect);
	}

	public static void onDirectHit(DirectHitEffect effect) {
		DIRECT_HIT.add(effect);
	}

	public static void onKill(KillEffect effect) {
		KILL.add(effect);
	}

	public static void onServerTick(Runnable task) {
		SERVER_TICK.add(task);
	}

	/** Runs {@code task} on the server thread {@code ticks} ticks from now, at least one. */
	public static void later(int ticks, Runnable task) {
		LATER.add(new Scheduled(now + Math.max(1, ticks), task));
	}

	private static void runDue() {
		if (LATER.isEmpty()) {
			return;
		}
		// Tasks may schedule more, so take the due ones out before running any.
		List<Scheduled> due = new ArrayList<>();
		LATER.removeIf(scheduled -> scheduled.due() <= now && due.add(scheduled));
		for (Scheduled scheduled : due) {
			scheduled.task().run();
		}
	}

	static void swing(SwingResult swing) {
		for (SwingEffect effect : SWING) {
			effect.afterSwing(swing);
		}
	}

	private static void kill(LivingEntity victim, DamageSource source) {
		if (!(source.getEntity() instanceof ServerPlayer player) || victim instanceof Player || victim instanceof ArmorStand) {
			return;
		}
		AttackHand swing = ComboTracker.currentSwing(player);
		ItemStack weapon = swing != null ? swing.itemStack() : player.getMainHandItem();
		DamageSource outer = killingBlow;
		killingBlow = source;
		try {
			for (KillEffect effect : KILL) {
				effect.onKill(player, weapon, victim);
			}
		} finally {
			killingBlow = outer;
		}
	}

	/** The damage that made the kill whose {@link KillEffect}s are running, or null outside of them. */
	public static @Nullable DamageSource killingBlow() {
		return killingBlow;
	}

	static void directHit(ServerPlayer player, ItemStack weapon, LivingEntity target) {
		for (DirectHitEffect effect : DIRECT_HIT) {
			effect.onDirectHit(player, weapon, target);
		}
	}
}
