package dev.vadim.bcenchantments.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.vadim.bcenchantments.mob.Adaptation;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A weapon counts its power fires on itself ({@link Adaptation#USAGE}) mid-fight; without this the
 * changed stack would play the hand's swap animation after every count.
 */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
	@ModifyReturnValue(method = "shouldInstantlyReplaceVisibleItem", at = @At("RETURN"))
	private boolean bcenchantments$ignoreUsage(boolean replace, ItemStack shown, ItemStack held) {
		return replace || ItemStack.matchesIgnoringComponents(shown, held, type -> type == Adaptation.USAGE);
	}
}
