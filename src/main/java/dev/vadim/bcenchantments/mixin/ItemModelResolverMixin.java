package dev.vadim.bcenchantments.mixin;

import dev.vadim.bcenchantments.client.PowerEffects;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Picks the trim power effect a weapon shows; see {@link PowerEffects}. */
@Mixin(ItemModelResolver.class)
public abstract class ItemModelResolverMixin {
	@Inject(method = "updateForTopItem", at = @At("TAIL"))
	private void bcenchantments$powerEffect(ItemStackRenderState output, ItemStack item, ItemDisplayContext displayContext, Level level,
		ItemOwner owner, int seed, CallbackInfo ci) {
		Object effect = item.isEmpty() ? null : PowerEffects.effectFor(item);
		((PowerEffects.Holder) output).bcenchantments$setEffect(effect);
		if (effect != null) {
			// Slots cache still items; an animated one is drawn again every frame.
			output.setAnimated();
		}
	}
}
