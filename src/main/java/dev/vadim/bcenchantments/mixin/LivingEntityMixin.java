package dev.vadim.bcenchantments.mixin;

import dev.vadim.bcenchantments.combat.ComboTracker;
import dev.vadim.bcenchantments.mob.MobScaling;
import dev.vadim.bcenchantments.weapon.WeaponStyles;
import net.bettercombat.api.AttackHand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Scales the knockback of a swing's hits by its weapon style, such as the spear's: none for the stabs,
 * a hard push for the heavy thrust. Also keeps scaled monsters dying to the same falls as before.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@ModifyVariable(method = "knockback(DDDLnet/minecraft/world/damagesource/DamageSource;FZ)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private double bcenchantments$spearKnockback(double power, double original, double xd, double zd, DamageSource source, float damage, boolean fromEffect) {
		if (((LivingEntity) (Object) this).level().isClientSide()) {
			return power;
		}
		AttackHand swing = ComboTracker.currentSwing(source.getEntity());
		return swing == null ? power : power * WeaponStyles.knockbackFactor(swing);
	}

	@ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
	private float bcenchantments$scaledFall(float amount, ServerLevel level, DamageSource source) {
		return MobScaling.fallDamage((LivingEntity) (Object) this, source, amount);
	}
}
