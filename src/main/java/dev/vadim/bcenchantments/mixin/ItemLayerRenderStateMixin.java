package dev.vadim.bcenchantments.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.vadim.bcenchantments.client.PowerEffects;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * After a layer submits its quads, submits them once more with the weapon's trim power effect, so it
 * draws over the item's own pixels; see {@link PowerEffects}. Layers drawn by a special renderer
 * (shields, tridents) have no quads and get nothing.
 */
@Mixin(ItemStackRenderState.LayerRenderState.class)
public abstract class ItemLayerRenderStateMixin {
	@Shadow @Final private List<BakedQuad> quads;

	@Inject(method = "submit", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitItem(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemDisplayContext;III[ILjava/util/List;Lnet/minecraft/client/renderer/item/ItemStackRenderState$FoilType;)V",
		shift = At.Shift.AFTER))
	private void bcenchantments$powerEffect(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, int overlayCoords, int outlineColor,
		CallbackInfo ci) {
		Object effect = PowerEffects.submitting;
		if (effect == null || outlineColor != 0 || this.quads.isEmpty()) {
			return;
		}
		collector.submitItem(poseStack, ItemDisplayContext.NONE, lightCoords, overlayCoords, 0, ItemStackRenderState.LayerRenderState.EMPTY_TINTS,
			PowerEffects.quads(effect, this.quads), ItemStackRenderState.FoilType.NONE);
	}
}
