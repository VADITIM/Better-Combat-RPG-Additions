package dev.vadim.bcenchantments.mixin;

import dev.vadim.bcenchantments.RpgAdditions;
import dev.vadim.bcenchantments.perk.Perks;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Plays a short lightning strike around the icon of a Lightning-enchanted item every few seconds. */
@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiGraphicsExtractorMixin {
	@Unique private static final Identifier OVERLAY = RpgAdditions.id("textures/gui/lightning_overlay.png");
	@Unique private static final int SIZE = 24;
	@Unique private static final int FRAMES = 8;
	@Unique private static final long FRAME_MS = 70;
	@Unique private static final long PERIOD_MS = 3000;

	@Inject(method = "item(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;III)V", at = @At("RETURN"))
	private void bcenchantments$lightningOverlay(LivingEntity entity, Level level, ItemStack stack, int x, int y, int seed, CallbackInfo ci) {
		if (stack.isEmpty() || Perks.level(stack, Perks.LIGHTNING) <= 0) {
			return;
		}
		int frame = (int) (Util.getMillis() % PERIOD_MS / FRAME_MS);
		if (frame >= FRAMES) {
			return;
		}
		int offset = (SIZE - 16) / 2;
		((GuiGraphicsExtractor) (Object) this).blit(
			RenderPipelines.GUI_TEXTURED, OVERLAY,
			x - offset, y - offset, 0, frame * SIZE,
			SIZE, SIZE, SIZE, SIZE * FRAMES
		);
	}
}
