package dev.vadim.bcenchantments.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Hides vanilla's attack indicator under the crosshair and beside the hotbar. Better Combat swings on
 * its own timing, so the vanilla cooldown it shows is meaningless, and the charge bars use its spot.
 */
@Mixin(Hud.class)
public abstract class HudMixin {
	@ModifyExpressionValue(
		method = {"extractCrosshair", "extractItemHotbar"},
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;")
	)
	private Object bcenchantments$hideAttackIndicator(Object status) {
		return status instanceof AttackIndicatorStatus ? AttackIndicatorStatus.OFF : status;
	}
}
