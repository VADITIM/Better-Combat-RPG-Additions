package dev.vadim.bcenchantments.mixin;

import dev.vadim.bcenchantments.perk.Control;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A mob hanging stunned in the air (see {@link Control}) cannot land a melee hit. */
@Mixin(Mob.class)
public abstract class MobMixin {
	@Inject(method = "doHurtTarget", at = @At("HEAD"), cancellable = true)
	private void bcenchantments$stunned(ServerLevel level, Entity target, CallbackInfoReturnable<Boolean> cir) {
		if (Control.stunned((Mob) (Object) this)) {
			cir.setReturnValue(false);
		}
	}
}
