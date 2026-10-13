package dev.vadim.bcenchantments.mixin;

import dev.vadim.bcenchantments.weapon.WeaponStyles;
import net.bettercombat.api.AttackHand;
import net.bettercombat.logic.PlayerAttackHelper;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Lets each weapon style swap in its own attacks, such as the air crit and uppercut, on the client and the server alike. */
@Mixin(PlayerAttackHelper.class)
public abstract class PlayerAttackHelperMixin {
	@Inject(method = "getCurrentAttack", at = @At("RETURN"), cancellable = true)
	private static void bcenchantments$specialAttack(Player player, int comboCount, CallbackInfoReturnable<AttackHand> cir) {
		AttackHand hand = cir.getReturnValue();
		if (hand != null) {
			AttackHand replaced = WeaponStyles.replace(player, hand);
			if (replaced != hand) {
				cir.setReturnValue(replaced);
			}
		}
	}
}
