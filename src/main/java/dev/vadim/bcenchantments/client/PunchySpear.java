package dev.vadim.bcenchantments.client;

import dev.vadim.bcenchantments.weapon.SpearStyle;
import net.bettercombat.api.AttackHand;
import net.bettercombat.api.MinecraftClient_BetterCombat;
import net.bettercombat.logic.PlayerAttackHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import punchy.client.animation.AnimationIntentCoordinator;
import punchy.client.animation.PoseHandler;
import punchy.client.animation.PunchyAnimationManager;
import punchy.client.animation.data.AnimationClip;
import punchy.client.state.HandEquipStateMachine;
import punchy.client.state.SpearStateMachine;
import punchy.config.PunchyConfig;

/**
 * First-person spear combo when Punchy draws the arms. Punchy plays its one spear clip on every hit,
 * so each stab of {@link SpearStyle} gets its own clip here instead, started with the swing rather
 * than at the hit so the draw is visible, and stretched over the swing's cooldown so the thrust lands
 * on the hit. Between stabs the spear is held at the shoulder, aimed ahead. Once the combo ends, or a
 * swing breaks off, the spear attack's own way back to idle follows: the spear flips upright and spins
 * in the hand. The clips live in
 * {@code assets/minecraft/punchy/animations/bcenchantments.animation.json}.
 * <p>
 * Only loaded when Punchy is installed.
 */
public final class PunchySpear {
	private static final String RETURN_CLIP = "bcenchantments_spear_return";
	/** Ticks the held pose waits for the next stab before the spear returns to idle. */
	private static final int RETURN_DELAY = 4;
	/** Blend into a stab from whatever the arm was doing, in clip time (a stab clip is one swing long). */
	private static final float STAB_BLEND = 0.07F;
	private static final float CHAINED_BLEND = 0.02F;
	/** Blend into the return, in Punchy's own clip time. */
	private static final float RETURN_BLEND = 0.15F;

	private static String pendingClip;
	private static InteractionHand pendingHand;
	private static float pendingSeconds;

	private static AnimationClip active;
	private static InteractionHand activeHand;
	private static boolean returning;
	private static int idleTicks;

	private PunchySpear() {
	}

	/** Better Combat started a swing: queues its stab clip for Punchy's next tick. */
	public static void onAttackStart(LocalPlayer player, AttackHand hand) {
		int stab = stabIndex(hand.attack().animation());
		if (stab == 0) {
			return;
		}
		pendingClip = "bcenchantments_spear_attack_" + stab;
		pendingHand = hand.isOffHand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
		// The combo count still points at this swing, so this is the stab's own cooldown.
		pendingSeconds = PlayerAttackHelper.getAttackCooldownTicksCapped(player) / 20.0F;
	}

	/** Called at the start of Punchy's spear state machine tick, every client tick. */
	public static void tick(Minecraft client) {
		LocalPlayer player = client.player;
		if (player == null) {
			pendingClip = null;
			active = null;
			return;
		}
		if (pendingClip != null) {
			String name = pendingClip;
			pendingClip = null;
			playStab(client, name, pendingHand, pendingSeconds);
			return;
		}
		if (active == null) {
			return;
		}
		PoseHandler pose = PunchyAnimationManager.POSE_HANDLER;
		if (pose.getCurrentClip() != active) {
			// Something else took the arm over, like the spear's charge or an equip.
			active = null;
			return;
		}
		if (returning) {
			return;
		}
		if (!SpearStateMachine.isSpearStack(player.getItemInHand(activeHand))) {
			// Swapped away; Punchy's equip animation takes it from here.
			active = null;
			return;
		}
		if (((MinecraftClient_BetterCombat) client).getCurrentAttackHand() != null) {
			idleTicks = 0;
		} else if (++idleTicks >= RETURN_DELAY) {
			// The combo ended, or a swing broke off before it landed.
			playReturn(client);
		}
	}

	/** Whether the hit of the current swing belongs to a stab clip already playing, so Punchy's own spear clip stays out. */
	public static boolean ownsAttack() {
		return active != null && !returning && PunchyAnimationManager.POSE_HANDLER.getCurrentClip() == active;
	}

	private static void playStab(Minecraft client, String name, InteractionHand hand, float seconds) {
		if (!PunchyConfig.isBetterCombatCompatEnabled()
			|| !client.options.getCameraType().isFirstPerson()
			|| PunchyAnimationManager.isHandBlacklisted(client, hand)) {
			return;
		}
		AnimationClip clip = PunchyAnimationManager.resolveNamedClip(name, client, hand);
		if (clip == null) {
			return;
		}
		if (HandEquipStateMachine.isEquipInProgress()) {
			// Punchy drops pending animations while an equip runs, so a stab right after drawing the spear cuts the equip short.
			HandEquipStateMachine.abortEquipForAction(client, hand);
		}
		boolean chained = ownsAttack();
		// A stab clip is one swing long; Punchy advances it by its animation speed times this.
		float speed = 1.0F / Math.max(0.01F, seconds * PunchyAnimationManager.ANIMATION_SPEED);
		HumanoidArm arm = PunchyAnimationManager.getUseArm(client, hand);
		AnimationIntentCoordinator.request(AnimationIntentCoordinator.Source.TWO_HANDED_USE, () -> {
			PoseHandler pose = PunchyAnimationManager.POSE_HANDLER;
			PunchyAnimationManager.setSourceHand(client, hand);
			pose.setActiveArm(arm);
			pose.setLooping(false);
			pose.setBlendInDurationNext(chained ? CHAINED_BLEND : STAB_BLEND);
			pose.playWithBlendFromCurrent(clip, speed, true);
			active = clip;
			activeHand = hand;
			returning = false;
			idleTicks = 0;
		});
	}

	private static void playReturn(Minecraft client) {
		InteractionHand hand = activeHand;
		AnimationClip clip = PunchyAnimationManager.resolveNamedClip(RETURN_CLIP, client, hand);
		if (clip == null) {
			active = null;
			return;
		}
		HumanoidArm arm = PunchyAnimationManager.getUseArm(client, hand);
		AnimationIntentCoordinator.request(AnimationIntentCoordinator.Source.TWO_HANDED_USE, () -> {
			PoseHandler pose = PunchyAnimationManager.POSE_HANDLER;
			PunchyAnimationManager.setSourceHand(client, hand);
			pose.setActiveArm(arm);
			pose.setLooping(false);
			pose.setBlendInDurationNext(RETURN_BLEND);
			// The return clip runs at its own timeline speed, like the spear attack it comes from.
			pose.playWithBlendFromCurrent(clip, 1.0F, false);
			active = clip;
			returning = true;
		});
	}

	private static int stabIndex(String animation) {
		if (SpearStyle.STAB_RIGHT.equals(animation)) {
			return 1;
		}
		if (SpearStyle.STAB_LEFT.equals(animation)) {
			return 2;
		}
		if (SpearStyle.STAB_CENTER.equals(animation)) {
			return 3;
		}
		return SpearStyle.HEAVY.equals(animation) ? 4 : 0;
	}
}
