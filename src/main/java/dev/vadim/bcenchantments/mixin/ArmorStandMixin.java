package dev.vadim.bcenchantments.mixin;

import dev.vadim.bcenchantments.combat.ComboTracker;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Test dummies: armor stands ignore every damage except /kill, but a swing on one still counts for combos. */
@Mixin(ArmorStand.class)
public abstract class ArmorStandMixin {
	@Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
	private void bcenchantments$unbreakable(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
		if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return;
		}
		ComboTracker.armorStandHit((ArmorStand) (Object) this, source);
		cir.setReturnValue(false);
	}
}
