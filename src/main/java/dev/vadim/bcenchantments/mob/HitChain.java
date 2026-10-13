package dev.vadim.bcenchantments.mob;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

/**
 * Pressure damage. A monster's hit on a player is softened, but every further monster hit that lands
 * within {@link MobBalance#CHAIN_RESET_TICKS} of the last one hits harder, up to a cap. All monsters
 * feed the same chain, so being surrounded ramps up fast; two seconds without a monster hit ends it.
 */
public final class HitChain {
	private static final class Chain {
		long lastHit;
		int hits;
	}

	private static final Map<Player, Chain> CHAINS = new WeakHashMap<>();

	private HitChain() {
	}

	/** The damage a hit on {@code player} deals after the chain, counting the hit into it. Called only for hits that get through. */
	public static float scale(Player player, DamageSource source, float amount) {
		if (amount <= 0.0F || !(source.getEntity() instanceof Enemy)) {
			return amount;
		}
		long now = player.level().getGameTime();
		Chain chain = CHAINS.computeIfAbsent(player, p -> new Chain());
		if (now - chain.lastHit > MobBalance.CHAIN_RESET_TICKS) {
			chain.hits = 0;
		}
		float factor = Math.min(1.0F + MobBalance.CHAIN_STEP * chain.hits, MobBalance.CHAIN_CAP);
		chain.hits++;
		chain.lastHit = now;
		return amount * MobBalance.FIRST_HIT_DAMAGE * factor;
	}
}
