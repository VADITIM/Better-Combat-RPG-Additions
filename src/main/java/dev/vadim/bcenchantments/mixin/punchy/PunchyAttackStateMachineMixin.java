package dev.vadim.bcenchantments.mixin.punchy;

import dev.vadim.bcenchantments.client.PunchyClips;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hands Punchy the slam or uppercut clip in place of its next attack clip. The clip goes in through
 * Punchy's custom-attack lookup rather than its Better Combat one: Punchy keeps the off arm still for
 * any two-handed clip that comes from the Better Combat lookup, which would leave the critical slam's
 * left hand frozen.
 */
@Pseudo
@Mixin(targets = "punchy.client.state.AttackStateMachine")
public abstract class PunchyAttackStateMachineMixin {
	@Inject(method = "resolveBetterCombatClip", at = @At("HEAD"), cancellable = true, require = 0)
	private void bcenchantments$skipComboClip(ItemStack stack, InteractionHand hand, Minecraft minecraft, CallbackInfoReturnable<Object> cir) {
		if (PunchyClips.hasPending()) {
			cir.setReturnValue(null);
		}
	}

	@Inject(method = "resolveCustomAttackClip", at = @At("HEAD"), cancellable = true, require = 0)
	private void bcenchantments$specialClip(CallbackInfoReturnable<Object> cir) {
		Object clip = PunchyClips.take(Minecraft.getInstance(), InteractionHand.MAIN_HAND);
		if (clip != null) {
			cir.setReturnValue(clip);
		}
	}
}
