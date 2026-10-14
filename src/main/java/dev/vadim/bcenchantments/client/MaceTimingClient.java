package dev.vadim.bcenchantments.client;

import dev.vadim.bcenchantments.network.MaceTimingPayload;
import dev.vadim.bcenchantments.weapon.MaceStyle;
import dev.vadim.bcenchantments.weapon.MaceTiming;
import net.bettercombat.api.AttackHand;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * The client half of {@link MaceTiming}: judges each mace swing, lets a fresh press start one as early
 * as the Perfect Hit window, and tells {@link PowerEffects} when the mace should sparkle. Better Combat
 * only starts a swing near full strength, so {@code AttackInteractorMixin} asks {@link #passes} at its
 * two strength checks. A held attack key keeps Better Combat's own timing and never lands a Perfect Hit.
 */
public final class MaceTimingClient {
	/** Set while Better Combat handles a fresh press of the attack key, as opposed to a held one. */
	private static boolean pressing;
	/** A press came before the window; no Perfect Hit until the next swing. */
	private static boolean fumbled;
	/** The swing in progress started before Better Combat's own strength check would let it. */
	private static boolean early;

	private MaceTimingClient() {
	}

	public static void beginPress() {
		pressing = true;
	}

	public static void endPress() {
		pressing = false;
	}

	/** Better Combat is about to try a swing: a fresh press with the mace before the window fumbles it. */
	public static void beforeSwing(LocalPlayer player) {
		if (pressing && holdsMace(player) && player.getAttackStrengthScale(0.0F) < MaceTiming.OPEN) {
			fumbled = true;
		}
	}

	/**
	 * The strength Better Combat's check reads: full for a fresh mace press once the window has opened
	 * ({@code landing} false), and full again when that early swing lands.
	 */
	public static float passes(@Nullable LocalPlayer player, float scale, boolean landing) {
		if (player == null || !holdsMace(player)) {
			return scale;
		}
		if (landing) {
			return early ? 1.0F : scale;
		}
		return pressing && scale >= MaceTiming.OPEN ? 1.0F : scale;
	}

	/** A swing started: decide its timing and tell the server. */
	public static void onAttackStart(Player player, AttackHand hand) {
		if (!(player instanceof LocalPlayer local) || !MaceStyle.INSTANCE.matches(hand.itemStack())) {
			return;
		}
		float scale = local.getAttackStrengthScale(0.0F);
		boolean perfect = pressing && !fumbled && MaceTiming.inWindow(scale, local.getCurrentItemAttackStrengthDelay());
		early = scale < 1.0F;
		fumbled = false;
		ClientPlayNetworking.send(new MaceTimingPayload(perfect));
	}

	/**
	 * How far into the Perfect Hit window {@code stack} is, in ticks, if it is the local player's mace
	 * and the window is open for it; -1 otherwise.
	 */
	public static float sparkle(ItemStack stack) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null || fumbled || stack != player.getMainHandItem() || !holdsMace(player)) {
			return -1.0F;
		}
		float delay = player.getCurrentItemAttackStrengthDelay();
		float partial = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
		float scale = player.getAttackStrengthScale(partial);
		return MaceTiming.inWindow(scale, delay) ? (scale - MaceTiming.OPEN) * delay : -1.0F;
	}

	private static boolean holdsMace(LocalPlayer player) {
		return MaceStyle.INSTANCE.matches(player.getMainHandItem());
	}
}
