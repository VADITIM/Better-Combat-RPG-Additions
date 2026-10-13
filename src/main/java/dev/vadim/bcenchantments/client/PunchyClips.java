package dev.vadim.bcenchantments.client;

import dev.vadim.bcenchantments.weapon.SwordStyle;
import java.lang.reflect.Method;
import net.bettercombat.api.AttackHand;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionHand;

/**
 * First-person clips for the special attacks when Punchy draws the arms. Punchy's Better Combat compat
 * stops Better Combat's own first-person animation and plays a clip of its own, chosen by combo index,
 * so the clip for a slam or uppercut is handed to it here. The clips live in
 * {@code assets/minecraft/punchy/animations/bcenchantments.animation.json}. Punchy is optional, so it
 * is only reached through reflection.
 */
public final class PunchyClips {
	private static final long PENDING_MS = 500;

	private static String pending;
	private static long pendingAt;
	private static Method resolveNamedClip;
	private static boolean lookupFailed;

	private PunchyClips() {
	}

	public static void onAttackStart(AttackHand hand) {
		String animation = hand.attack().animation();
		if (SwordStyle.UPPERCUT.equals(animation)) {
			pending = "bcenchantments_uppercut";
		} else if (SwordStyle.SLAM_CRIT.equals(animation)) {
			pending = "bcenchantments_slam_crit";
		} else if (SwordStyle.SLAM.equals(animation)) {
			pending = "bcenchantments_slam";
		} else {
			pending = null;
		}
		pendingAt = Util.getMillis();
	}

	/** Whether Punchy is about to pick a clip for a special attack. */
	public static boolean hasPending() {
		return pending != null && Util.getMillis() - pendingAt < PENDING_MS;
	}

	/** The Punchy clip for the pending special attack, or null to let Punchy choose. */
	public static Object take(Minecraft minecraft, InteractionHand hand) {
		if (!hasPending()) {
			return null;
		}
		String name = pending;
		pending = null;
		Method method = resolveNamedClip();
		if (method == null) {
			return null;
		}
		try {
			return method.invoke(null, name, minecraft, hand);
		} catch (ReflectiveOperationException | RuntimeException e) {
			return null;
		}
	}

	private static Method resolveNamedClip() {
		if (resolveNamedClip == null && !lookupFailed) {
			try {
				resolveNamedClip = Class.forName("punchy.client.animation.PunchyAnimationManager")
					.getMethod("resolveNamedClip", String.class, Minecraft.class, InteractionHand.class);
			} catch (ReflectiveOperationException e) {
				lookupFailed = true;
			}
		}
		return resolveNamedClip;
	}
}
