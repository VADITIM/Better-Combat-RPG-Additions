package dev.vadim.bcenchantments.perk;

import dev.vadim.bcenchantments.Particles;
import dev.vadim.bcenchantments.TrimEnchantments;
import dev.vadim.bcenchantments.mob.Adaptation;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;

/**
 * Killing a monster sends the player on a rampage: faster attacks for a few seconds, refreshed by
 * every further kill, so a fight through a crowd keeps speeding up. The boost is a transient
 * modifier, so nothing stays on a player who logs out mid-rampage.
 */
final class Rampaging {
	private static final Identifier RAMPAGE = TrimEnchantments.id("rampaging");
	/** Attack speed gained for levels 1 to 3, as a share of the total. */
	private static final double[] SPEED = {0.2, 0.3, 0.4};
	/** How long the rampage lasts after the last kill: 4 s. */
	private static final int TICKS = 80;

	/** Players on a rampage, until the tick it ends. */
	private static final Map<ServerPlayer, Integer> RAMPAGING = new WeakHashMap<>();

	private Rampaging() {
	}

	static void onKill(ServerPlayer player, ItemStack weapon, LivingEntity victim) {
		int level = Math.min(Perks.level(weapon, Perks.RAMPAGING), SPEED.length);
		if (level <= 0 || !(victim instanceof Enemy) || Adaptation.resists(victim, Perks.RAMPAGING)) {
			return;
		}
		Adaptation.used(player, weapon, Perks.RAMPAGING);
		AttributeInstance speed = player.getAttribute(Attributes.ATTACK_SPEED);
		if (speed == null) {
			return;
		}
		AttributeModifier current = speed.getModifier(RAMPAGE);
		double amount = SPEED[level - 1];
		if (current == null || current.amount() < amount) {
			speed.addOrUpdateTransientModifier(new AttributeModifier(RAMPAGE, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		}
		if (current == null) {
			player.level().playSound((Entity) null, player.getX(), player.getY(), player.getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 0.4F, 1.5F);
		}
		Particles.send(player.level(), ParticleTypes.ANGRY_VILLAGER, player.getX(), player.getY(1.1), player.getZ(), 2, 0.3, 0.2, 0.3, 0.0);
		RAMPAGING.put(player, player.tickCount + TICKS);
	}

	/** Whether {@code player} is on a rampage right now. */
	static boolean active(ServerPlayer player) {
		Integer until = RAMPAGING.get(player);
		return until != null && player.tickCount <= until;
	}

	static void tick() {
		Iterator<Map.Entry<ServerPlayer, Integer>> it = RAMPAGING.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<ServerPlayer, Integer> entry = it.next();
			ServerPlayer player = entry.getKey();
			if (player.isRemoved() || !player.isAlive() || player.tickCount > entry.getValue()) {
				AttributeInstance speed = player.getAttribute(Attributes.ATTACK_SPEED);
				if (speed != null) {
					speed.removeModifier(RAMPAGE);
				}
				it.remove();
			}
		}
	}
}
