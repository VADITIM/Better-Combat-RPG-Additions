package dev.vadim.bcenchantments.mixin;

import dev.vadim.bcenchantments.client.PowerEffects;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Holds the trim power effect of the stack the state was resolved from, and exposes it to the layers
 * while they submit; see {@link PowerEffects}.
 */
@Mixin(ItemStackRenderState.class)
public abstract class ItemStackRenderStateMixin implements PowerEffects.Holder {
	@Unique private @Nullable Object bcenchantments$effect;

	@Override
	public @Nullable Object bcenchantments$effect() {
		return this.bcenchantments$effect;
	}

	@Override
	public void bcenchantments$setEffect(@Nullable Object effect) {
		this.bcenchantments$effect = effect;
	}

	@Inject(method = "clear", at = @At("HEAD"))
	private void bcenchantments$clearEffect(CallbackInfo ci) {
		this.bcenchantments$effect = null;
	}

	@Inject(method = "submit", at = @At("HEAD"))
	private void bcenchantments$beginSubmit(CallbackInfo ci) {
		PowerEffects.submitting = this.bcenchantments$effect;
	}

	@Inject(method = "submit", at = @At("TAIL"))
	private void bcenchantments$endSubmit(CallbackInfo ci) {
		PowerEffects.submitting = null;
	}
}
