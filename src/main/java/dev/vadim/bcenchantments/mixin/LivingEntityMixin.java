package dev.vadim.bcenchantments.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import dev.vadim.bcenchantments.combat.ComboTracker;
import dev.vadim.bcenchantments.mob.MobScaling;
import dev.vadim.bcenchantments.perk.ShieldPowers;
import dev.vadim.bcenchantments.perk.Updraft;
import dev.vadim.bcenchantments.weapon.WeaponStyles;
import net.bettercombat.api.AttackHand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Scales the knockback of a swing's hits by its weapon style, such as the spear's: none for the stabs,
 * a hard push for the heavy thrust. Also keeps scaled monsters dying to the same falls as before, and
 * sets off a trimmed shield's power when it blocks a hit, and keeps Lunge on the ground unless Updraft
 * launched the player.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	/** The shield in use when a hit arrives; blocking can knock it out of use before the hit is done. */
	@Unique
	private ItemStack bcenchantments$blockingWith;

	@Shadow
	public abstract ItemStack getItemBlockingWith();

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

	/** No Lunge in the air, except after an Updraft launch. */
	@WrapWithCondition(method = "postPiercingAttack", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;doPostPiercingAttackEffects(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;)V"))
	private boolean bcenchantments$groundedLunge(ServerLevel level, LivingEntity entity) {
		return !(entity instanceof ServerPlayer player) || Updraft.lungeAllowed(player);
	}

	@Inject(method = "applyItemBlocking", at = @At("HEAD"))
	private void bcenchantments$rememberShield(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Float> cir) {
		this.bcenchantments$blockingWith = this.getItemBlockingWith();
	}

	@Inject(method = "applyItemBlocking", at = @At("RETURN"))
	private void bcenchantments$shieldPower(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Float> cir) {
		ItemStack shield = this.bcenchantments$blockingWith;
		this.bcenchantments$blockingWith = null;
		if (shield != null && cir.getReturnValueF() > 0.0F) {
			ShieldPowers.onBlock((LivingEntity) (Object) this, source, shield);
		}
	}
}
