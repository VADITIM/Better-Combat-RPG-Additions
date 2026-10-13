package dev.vadim.bcenchantments.perk;

import dev.vadim.bcenchantments.combat.CombatHooks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Whatever the player kills bursts like the Wind Charged effect, but the burst hurts. Kills by the
 * weapon's lightning, echoes or other bursts count too, so a packed crowd can go off in a chain.
 * With {@link Synergy#FROST_BURST} the burst chills what it catches.
 */
final class WindCharged {
	private static final double RADIUS = 2.5;
	/** Burst damage for levels 1 to 3. */
	private static final float[] DAMAGE = {3.0F, 4.0F, 5.0F};

	private WindCharged() {
	}

	static void onKill(ServerPlayer player, ItemStack weapon, LivingEntity victim) {
		int level = Perks.level(weapon, Perks.WIND_CHARGED);
		if (level <= 0) {
			return;
		}
		float damage = DAMAGE[Math.min(level, DAMAGE.length) - 1];
		int cold = Synergy.FROST_BURST.on(weapon) ? Perks.level(weapon, Perks.COLD_EDGE) : 0;
		Vec3 at = victim.position().add(0.0, victim.getBbHeight() * 0.5, 0.0);
		// Next tick, so a burst that kills does not die again inside this death.
		CombatHooks.later(1, () -> {
			if (!player.isRemoved() && player.level() == victim.level()) {
				WindBurst.at(player, at, RADIUS, damage, null, caught -> {
					if (cold > 0) {
						ColdEdge.chill(caught, cold);
					}
				});
			}
		});
	}
}
