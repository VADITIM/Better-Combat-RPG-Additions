package dev.vadim.bcenchantments.mixin.punchy;

import dev.vadim.bcenchantments.client.PunchySpear;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Runs the spear combo's first-person clips from Punchy's spear tick, and keeps Punchy's own spear
 * clip from replacing a stab when its hit lands.
 */
@Pseudo
@Mixin(targets = "punchy.client.state.SpearStateMachine")
public abstract class PunchySpearStateMachineMixin {
	@Inject(method = "tick", at = @At("HEAD"), require = 0)
	private void bcenchantments$comboClips(Minecraft client, CallbackInfo ci) {
		PunchySpear.tick(client);
	}

	@Inject(method = "resolveSpearAttackClip", at = @At("HEAD"), cancellable = true, require = 0)
	private void bcenchantments$keepStabClip(Minecraft client, InteractionHand hand, CallbackInfoReturnable<Object> cir) {
		if (PunchySpear.ownsAttack()) {
			cir.setReturnValue(null);
		}
	}
}
