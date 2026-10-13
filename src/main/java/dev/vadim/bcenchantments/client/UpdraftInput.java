package dev.vadim.bcenchantments.client;

import dev.vadim.bcenchantments.network.AirMovePayload;
import dev.vadim.bcenchantments.network.UpdraftLaunchPayload;
import dev.vadim.bcenchantments.perk.Updraft;
import net.bettercombat.api.MinecraftClient_BetterCombat;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;

/**
 * Reads the right click for Updraft's air move ({@link Updraft}) before vanilla and Better Combat
 * see it, and sends it to the server. Better Combat cancels right clicks while the weapon recovers,
 * so the vanilla use path never reaches the server right after a launch.
 * <p>
 * While a move runs ({@link #blocking}), {@code MinecraftMixin} and {@code AttackInteractorMixin}
 * drop every left click, Better Combat swing and use of either hand; the server refuses them too.
 */
public final class UpdraftInput {
	/** Whether the move was used this airtime, and the move under way with the tick it started. */
	private static boolean used;
	private static Updraft.Move move;
	private static int started;
	private static boolean wasDown;
	/** The right click that started the move is still held; it must not go on to use an item. */
	private static boolean held;
	/** Tick the server said Updraft launched the player, or -1; the spear's lunge needs it. */
	private static int launchedAt = -1;
	private static final int LAUNCH_GRACE_TICKS = 3;

	private UpdraftInput() {
	}

	static void register() {
		ClientTickEvents.START_CLIENT_TICK.register(UpdraftInput::tick);
		ClientPlayNetworking.registerGlobalReceiver(UpdraftLaunchPayload.TYPE, (payload, context) -> {
			LocalPlayer player = context.client().player;
			launchedAt = player == null ? -1 : player.tickCount;
		});
	}

	private static void tick(Minecraft client) {
		LocalPlayer player = client.player;
		KeyMapping use = client.options.keyUse;
		boolean down = use.isDown();
		boolean pressed = down && !wasDown;
		wasDown = down;
		if (!down) {
			held = false;
		}
		if (player == null) {
			used = false;
			move = null;
			return;
		}
		// A launch only ends on a landing after it: the launch tick itself can still read as grounded.
		if (launchedAt >= 0 && (player.tickCount < launchedAt || player.tickCount - launchedAt > LAUNCH_GRACE_TICKS && !Updraft.airborne(player))) {
			launchedAt = -1;
		}
		if (!Updraft.airborne(player)) {
			used = false;
			move = null;
			return;
		}
		if (move == Updraft.Move.LUNGE && player.tickCount - started > Updraft.LUNGE_TICKS) {
			move = null;
		}
		if (used || client.gui.screen() != null || !ClientPlayNetworking.canSend(AirMovePayload.TYPE)) {
			return;
		}
		InteractionHand hand = InteractionHand.MAIN_HAND;
		boolean launched = launchedAt >= 0;
		Updraft.Move found = Updraft.moveFor(player, player.getMainHandItem(), launched);
		if (found == null) {
			hand = InteractionHand.OFF_HAND;
			found = Updraft.moveFor(player, player.getOffhandItem(), launched);
		}
		if (found == null) {
			return;
		}
		// A click shorter than a tick only shows as a queued click; take it so vanilla never uses the item.
		boolean clicked = false;
		while (use.consumeClick()) {
			clicked = true;
		}
		if (!clicked && !pressed) {
			return;
		}
		used = true;
		held = true;
		move = found;
		started = player.tickCount;
		// A swing already winding up would land during the move; drop it.
		((MinecraftClient_BetterCombat) client).cancelUpswing();
		player.swing(hand);
		ClientPlayNetworking.send(new AirMovePayload(hand == InteractionHand.OFF_HAND));
	}

	/** Whether clicks are blocked: a slam or dive until it lands, a lunge for its first ticks. */
	public static boolean blocking() {
		return move != null;
	}

	/** Whether item use is blocked: during a move, and while its right click stays held. */
	public static boolean blocksUse() {
		return move != null || held;
	}
}
