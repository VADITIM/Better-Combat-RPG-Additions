package dev.vadim.bcenchantments.mixin.punchy;

import dev.vadim.bcenchantments.client.PunchyClips;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A slam is swung while falling, which Punchy treats as a critical hit and answers with its critical
 * clip before it asks for the Better Combat one. Skip the critical clip while a special attack waits.
 */
@Pseudo
@Mixin(targets = "punchy.client.state.CustomAnimationResolver")
public abstract class PunchyCustomAnimationResolverMixin {
	@Inject(method = "resolveCriticalName", at = @At("HEAD"), cancellable = true, require = 0)
	private static void bcenchantments$skipCritical(CallbackInfoReturnable<String> cir) {
		if (PunchyClips.hasPending()) {
			cir.setReturnValue(null);
		}
	}
}
