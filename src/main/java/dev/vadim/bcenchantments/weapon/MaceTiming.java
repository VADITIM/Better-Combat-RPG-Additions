package dev.vadim.bcenchantments.weapon;

import dev.vadim.bcenchantments.Particles;
import dev.vadim.bcenchantments.network.MaceTimingPayload;
import java.util.Map;
import java.util.WeakHashMap;
import net.bettercombat.api.AttackHand;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

/**
 * The mace's timing. It readies {@link #COOLDOWN} times slower than vanilla, and at {@link #OPEN} of
 * that wait it sparkles for {@link #WINDOW_TICKS}: a swing started then is a Perfect Hit and deals
 * {@link #PERFECT} times the damage. Every other swing, too early or too late, deals
 * {@link #MISSED} times. A press before the window fumbles it until the next swing, so spamming the
 * attack key never lands one. The swing still has the attack strength of a full one, whenever it lands.
 *
 * <p>The client judges the timing ({@code client/MaceTimingClient}) and tells the server as the swing
 * starts; Better Combat takes the client's targets on trust the same way. The server applies the
 * multiplier to every hit of the mace swing that follows.
 */
public final class MaceTiming {
	/** How much longer the mace takes to ready than vanilla's. */
	public static final float COOLDOWN = 1.3F;
	/** Where the Perfect Hit window opens, as a share of the ready time. */
	public static final float OPEN = 0.8F;
	/** How long the window stays open: 0.15 seconds. */
	public static final int WINDOW_TICKS = 3;
	public static final float PERFECT = 1.5F;
	public static final float MISSED = 0.9F;

	private static final Map<ServerPlayer, Boolean> PENDING = new WeakHashMap<>();
	// Swings run synchronously on the server thread, like ComboTracker's.
	private static @Nullable ServerPlayer swingPlayer;
	private static boolean swingPerfect;

	private MaceTiming() {
	}

	public static void init() {
		PayloadTypeRegistry.serverboundPlay().register(MaceTimingPayload.TYPE, MaceTimingPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(MaceTimingPayload.TYPE, (payload, context) -> PENDING.put(context.player(), payload.perfect()));
	}

	/** Whether {@code scale}, an attack strength scale of a weapon readying over {@code delay} ticks, is in the window. */
	public static boolean inWindow(float scale, float delay) {
		return scale >= OPEN && scale < OPEN + WINDOW_TICKS / delay;
	}

	public static void beginSwing(ServerPlayer player, AttackHand hand) {
		if (MaceStyle.INSTANCE.matches(hand.itemStack())) {
			swingPlayer = player;
			Boolean perfect = PENDING.remove(player);
			swingPerfect = perfect != null && perfect;
		}
	}

	public static void endSwing() {
		swingPlayer = null;
	}

	/** Whether {@code player} is running a timed mace swing on the server right now. */
	public static boolean timed(Player player) {
		return player == swingPlayer;
	}

	/** The damage factor of {@code player}'s hit on {@code target}, with a burst of sparks on a Perfect Hit. */
	public static float damage(Player player, Entity target, float amount) {
		if (player != swingPlayer) {
			return amount;
		}
		if (swingPerfect && player.level() instanceof ServerLevel level) {
			Particles.send(level, ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY(0.6), target.getZ(), 24, 0.35, 0.4, 0.35, 0.6);
		}
		return amount * (swingPerfect ? PERFECT : MISSED);
	}
}
