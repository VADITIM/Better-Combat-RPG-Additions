package dev.vadim.bcenchantments.mixin;

import dev.vadim.bcenchantments.progression.Refinement;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Trim powers combine only at the smithing table. The anvil keeps the left item's powers and would
 * silently drop the right one's, so it refuses a right-hand item that carries any.
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin extends ItemCombinerMenu {
	@Shadow
	@Final
	private DataSlot cost;

	private AnvilMenuMixin() {
		super(null, 0, null, null, null);
	}

	@Inject(method = "createResult", at = @At("TAIL"))
	private void bcenchantments$keepPowers(CallbackInfo ci) {
		if (!Refinement.powersOf(this.inputSlots.getItem(1)).isEmpty()) {
			this.resultSlots.setItem(0, ItemStack.EMPTY);
			this.cost.set(0);
		}
	}
}
