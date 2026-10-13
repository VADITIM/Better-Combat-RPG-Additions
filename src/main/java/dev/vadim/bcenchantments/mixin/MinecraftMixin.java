package dev.vadim.bcenchantments.mixin;

import dev.vadim.bcenchantments.client.UpdraftInput;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Drops left clicks, held attacks and item use during Updraft's air move ({@link UpdraftInput}).
 * Applied before Better Combat's hooks on the same methods, so its attacks never start either.
 */
@Mixin(value = Minecraft.class, priority = 100)
public abstract class MinecraftMixin {
	@Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
	private void bcenchantments$airMoveAttack(CallbackInfoReturnable<Boolean> cir) {
		if (UpdraftInput.blocking()) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
	private void bcenchantments$airMoveHeldAttack(boolean down, CallbackInfo ci) {
		if (UpdraftInput.blocking()) {
			ci.cancel();
		}
	}

	@Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
	private void bcenchantments$airMoveUse(CallbackInfo ci) {
		if (UpdraftInput.blocksUse()) {
			ci.cancel();
		}
	}
}
