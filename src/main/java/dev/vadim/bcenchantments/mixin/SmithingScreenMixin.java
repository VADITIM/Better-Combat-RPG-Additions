package dev.vadim.bcenchantments.mixin;

import dev.vadim.bcenchantments.progression.Refinement;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.ItemCombinerScreen;
import net.minecraft.client.gui.screens.inventory.SmithingScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SmithingMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Shows what a new trim power costs in experience levels, like the anvil, red when the player has too few. */
@Mixin(SmithingScreen.class)
public abstract class SmithingScreenMixin extends ItemCombinerScreen<SmithingMenu> {
	private static final int AFFORDABLE = 0xFF80FF20;
	private static final int TOO_EXPENSIVE = 0xFFFF6060;

	private SmithingScreenMixin() {
		super(null, null, null, null);
	}

	@Inject(method = "extractLabels", at = @At("TAIL"))
	private void bcenchantments$levelCost(GuiGraphicsExtractor g, int mouseX, int mouseY, CallbackInfo ci) {
		Player player = this.minecraft.player;
		if (player == null) {
			return;
		}
		Refinement.Plan plan = Refinement.plan(this.menu.getSlot(0).getItem(), this.menu.getSlot(1).getItem(), this.menu.getSlot(2).getItem(),
			player.getEnchantmentSeed());
		if (plan == null || plan.levelCost() <= 0) {
			return;
		}
		boolean affordable = player.hasInfiniteMaterials() || player.experienceLevel >= plan.levelCost();
		Component cost = Component.translatable("container.repair.cost", plan.levelCost());
		g.text(this.font, cost, 44, 30, affordable ? AFFORDABLE : TOO_EXPENSIVE, true);
	}
}
