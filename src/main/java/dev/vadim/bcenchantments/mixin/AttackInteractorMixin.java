package dev.vadim.bcenchantments.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.vadim.bcenchantments.client.ComboClock;
import dev.vadim.bcenchantments.client.ComboRest;
import dev.vadim.bcenchantments.client.MaceTimingClient;
import dev.vadim.bcenchantments.client.UpdraftInput;
import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.client.AttackInteractor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets the combo meter read how long Better Combat keeps the combo alive, holds swings back during
 * Updraft's air move and a combo's rest, and lets the mace swing in its Perfect Hit window.
 */
@Mixin(AttackInteractor.class)
public abstract class AttackInteractorMixin implements ComboClock.Source {
	@Shadow
	private int lastAttacked;
	@Shadow
	private int comboReset;

	@Inject(method = "<init>", at = @At("RETURN"))
	private void bcenchantments$register(Minecraft client, CallbackInfo ci) {
		ComboClock.source = this;
	}

	/**
	 * Better Combat starts swings from its own tick too (a held attack key); none start during Updraft's
	 * air move or a weapon's rest after its combo.
	 */
	@Inject(method = "startUpswing", at = @At("HEAD"), cancellable = true)
	private void bcenchantments$airMove(WeaponAttributes attributes, CallbackInfo ci) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (UpdraftInput.blocking() || player != null && ComboRest.resting(player)) {
			ci.cancel();
			return;
		}
		if (player != null) {
			MaceTimingClient.beforeSwing(player);
		}
	}

	/** Tells the mace's timing that this swing comes from a fresh press rather than a held key. */
	@Inject(method = "onDoAttack", at = @At("HEAD"))
	private void bcenchantments$beginPress(CallbackInfoReturnable<Boolean> cir) {
		MaceTimingClient.beginPress();
	}

	@Inject(method = "onDoAttack", at = @At("RETURN"))
	private void bcenchantments$endPress(CallbackInfoReturnable<Boolean> cir) {
		MaceTimingClient.endPress();
	}

	/** A fresh press swings the mace from the Perfect Hit window on, not only near full strength. */
	@ModifyExpressionValue(method = "startUpswing", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getAttackStrengthScale(F)F"))
	private float bcenchantments$maceStart(float scale) {
		return MaceTimingClient.passes(Minecraft.getInstance().player, scale, false);
	}

	/** ...and lets that early swing land, which Better Combat checks against the same strength. */
	@ModifyExpressionValue(method = "performAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getAttackStrengthScale(F)F"))
	private float bcenchantments$maceLand(float scale) {
		return MaceTimingClient.passes(Minecraft.getInstance().player, scale, true);
	}

	@Override
	public int bcenchantments$lastAttacked() {
		return lastAttacked;
	}

	@Override
	public int bcenchantments$comboReset() {
		return comboReset;
	}
}
