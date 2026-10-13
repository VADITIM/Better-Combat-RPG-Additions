package dev.vadim.bcenchantments.combat;

import java.util.List;
import net.bettercombat.api.AttackHand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * One finished Better Combat swing that landed.
 *
 * @param hits everything the swing took health off, in the order it was hit
 * @param primary the hit the player aimed at, or the first one when the cursor was on none of them
 */
public record SwingResult(ServerPlayer player, AttackHand hand, ItemStack weapon, List<LivingEntity> hits, LivingEntity primary) {
	public String animation() {
		return this.hand.attack().animation();
	}
}
