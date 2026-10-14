package dev.vadim.bcenchantments.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.vadim.bcenchantments.combat.ComboTracker;
import dev.vadim.bcenchantments.combat.CriticalHits;
import dev.vadim.bcenchantments.mob.HitChain;
import dev.vadim.bcenchantments.perk.Updraft;
import dev.vadim.bcenchantments.weapon.MaceTiming;
import dev.vadim.bcenchantments.weapon.TridentStyle;
import dev.vadim.bcenchantments.weapon.WeaponStyles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tells a running swing whether each of its attacks actually took health off the target, and swaps
 * vanilla's falling crit for the rolled one from {@link CriticalHits}. Also lets weapon styles give
 * each attack its own cooldown, runs monster hits through {@link HitChain}, and applies the mace's
 * {@link MaceTiming} and the trident's Perfect Spacing ({@link TridentStyle}).
 */
@Mixin(Player.class)
public abstract class PlayerMixin {
	@ModifyReturnValue(method = "createAttributes", at = @At("RETURN"))
	private static AttributeSupplier.Builder bcenchantments$criticalAttributes(AttributeSupplier.Builder builder) {
		return builder.add(CriticalHits.CRITICAL_CHANCE).add(CriticalHits.CRITICAL_DAMAGE);
	}

	/** A timed mace swing hits with full strength whenever it lands; its timing sets the damage instead. */
	@ModifyReturnValue(method = "getAttackStrengthScale", at = @At("RETURN"))
	private float bcenchantments$maceStrength(float scale) {
		return MaceTiming.timed((Player) (Object) this) ? 1.0F : scale;
	}

	@ModifyArg(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurtOrSimulate(Lnet/minecraft/world/damagesource/DamageSource;F)Z"), index = 1)
	private float bcenchantments$maceTiming(float amount, @Local(argsOnly = true) Entity target) {
		Player player = (Player) (Object) this;
		return TridentStyle.damage(player, target, MaceTiming.damage(player, target, amount));
	}

	@ModifyReturnValue(method = "getCurrentItemAttackStrengthDelay", at = @At("RETURN"))
	private float bcenchantments$styleCooldown(float ticks) {
		return ticks * WeaponStyles.cooldownFactor((Player) (Object) this);
	}

	/** After the hurt cooldown and shield checks, so only hits that get through feed the chain. */
	@ModifyVariable(method = "actuallyHurt", at = @At("HEAD"), argsOnly = true)
	private float bcenchantments$hitChain(float amount, ServerLevel level, DamageSource source) {
		return HitChain.scale((Player) (Object) this, source, amount);
	}

	@Inject(method = "attack", at = @At("HEAD"), cancellable = true)
	private void bcenchantments$beforeAttack(Entity target, CallbackInfo ci) {
		if ((Object) this instanceof ServerPlayer player) {
			// No hit lands during Updraft's air move, whichever hand swung.
			if (Updraft.busy(player)) {
				ci.cancel();
				return;
			}
			ComboTracker.beforeAttack(player, target);
		}
	}

	@Inject(method = "attack", at = @At("RETURN"))
	private void bcenchantments$afterAttack(Entity target, CallbackInfo ci) {
		if ((Object) this instanceof ServerPlayer player) {
			ComboTracker.afterAttack(player, target);
		}
	}

	@ModifyExpressionValue(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;canCriticalAttack(Lnet/minecraft/world/entity/Entity;)Z"))
	private boolean bcenchantments$rolledCritical(boolean falling, Entity target) {
		// The client's guess would be the falling one; the server sends the crit effects anyway.
		return (Object) this instanceof ServerPlayer player && CriticalHits.isCritical(player, target);
	}

	@ModifyConstant(method = "attack", constant = @Constant(floatValue = 1.5F))
	private float bcenchantments$criticalDamage(float vanilla) {
		return CriticalHits.damageMultiplier((Player) (Object) this);
	}
}
