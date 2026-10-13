package dev.vadim.bcenchantments.mixin;

import dev.vadim.bcenchantments.client.GuideScreen;
import dev.vadim.bcenchantments.client.SmithingPanel;
import dev.vadim.bcenchantments.progression.Refinement;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.ItemCombinerScreen;
import net.minecraft.client.gui.screens.inventory.SmithingScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SmithingMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Shows what a new trim power costs in experience levels, like the anvil, red when the player has too few,
 * and lays out the side panel ({@link SmithingPanel}) like the recipe book: open, it sits on the left and
 * pushes the table right, the two centred together. A toggle and the Field Guide's "?" stand on the
 * table's right.
 */
@Mixin(SmithingScreen.class)
public abstract class SmithingScreenMixin extends ItemCombinerScreen<SmithingMenu> {
	private static final int AFFORDABLE = 0xFF80FF20;
	private static final int TOO_EXPENSIVE = 0xFFFF6060;

	@Unique
	private Button bcenchantments$toggle;
	@Unique
	private Button bcenchantments$guide;

	private SmithingScreenMixin() {
		super(null, null, null, null);
	}

	// ItemCombinerScreen.init sets leftPos and then calls subInit, so moving the table here takes effect before anything reads it.
	@Inject(method = "subInit", at = @At("HEAD"))
	private void bcenchantments$layout(CallbackInfo ci) {
		this.bcenchantments$toggle = this.addRenderableWidget(Button.builder(bcenchantments$toggleLabel(), button -> {
			SmithingPanel.toggle();
			bcenchantments$place();
		}).bounds(0, 0, 20, 20).build());
		this.bcenchantments$guide = this.addRenderableWidget(GuideScreen.button(this, 0, 0));
		bcenchantments$place();
	}

	@Unique
	private void bcenchantments$place() {
		this.leftPos = SmithingPanel.tableLeft(this.width);
		int x = this.leftPos + this.imageWidth + 4;
		this.bcenchantments$toggle.setPosition(x, this.topPos);
		this.bcenchantments$toggle.setMessage(bcenchantments$toggleLabel());
		this.bcenchantments$toggle.setTooltip(Tooltip.create(Component.translatable(
			SmithingPanel.isOpen() ? "panel.bcenchantments.hide" : "panel.bcenchantments.show")));
		this.bcenchantments$guide.setPosition(x, this.topPos + 24);
	}

	@Unique
	private static Component bcenchantments$toggleLabel() {
		return Component.literal(SmithingPanel.isOpen() ? "<" : ">");
	}

	// extractLabels is inherited from AbstractContainerScreen, which Mixin cannot target from here, so draw after the whole
	// screen at the label position (relative to the container origin).
	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void bcenchantments$levelCost(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		Player player = this.minecraft.player;
		if (player == null) {
			return;
		}
		SmithingPanel.extract((SmithingScreen) (Object) this, g, mouseX, mouseY, this.leftPos, this.topPos);
		Refinement.Plan plan = Refinement.plan(this.menu.getSlot(0).getItem(), this.menu.getSlot(1).getItem(), this.menu.getSlot(2).getItem(),
			player.getEnchantmentSeed());
		if (plan == null || plan.levelCost() <= 0) {
			return;
		}
		boolean affordable = player.hasInfiniteMaterials() || player.experienceLevel >= plan.levelCost();
		Component cost = Component.translatable("container.repair.cost", plan.levelCost());
		g.text(this.font, cost, this.leftPos + 44, this.topPos + 30, affordable ? AFFORDABLE : TOO_EXPENSIVE, true);
	}

	/** A click on the side panel is not a click outside the table, so it does not throw the carried item. */
	@Override
	protected boolean hasClickedOutside(double x, double y, int left, int top) {
		return !SmithingPanel.contains(x, y) && super.hasClickedOutside(x, y, left, top);
	}
}
