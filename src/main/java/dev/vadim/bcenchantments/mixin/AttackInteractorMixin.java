package dev.vadim.bcenchantments.mixin;

import dev.vadim.bcenchantments.client.ComboClock;
import net.bettercombat.client.AttackInteractor;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Lets the combo meter read how long Better Combat keeps the combo alive. */
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

	@Override
	public int bcenchantments$lastAttacked() {
		return lastAttacked;
	}

	@Override
	public int bcenchantments$comboReset() {
		return comboReset;
	}
}
