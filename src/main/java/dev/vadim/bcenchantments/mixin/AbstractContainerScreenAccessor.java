package dev.vadim.bcenchantments.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Where a container screen sits, for panels drawn beside it. */
@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
	@Accessor("leftPos")
	int bcenchantments$leftPos();

	@Accessor("topPos")
	int bcenchantments$topPos();

	@Accessor("imageWidth")
	int bcenchantments$imageWidth();
}
