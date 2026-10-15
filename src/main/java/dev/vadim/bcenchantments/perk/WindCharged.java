package dev.vadim.bcenchantments.perk;

import dev.vadim.bcenchantments.combat.CombatHooks;
import dev.vadim.bcenchantments.mob.Adaptation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Whatever the player kills bursts like the Wind Charged effect, but the burst hurts. Kills by the
 * weapon's lightning, echoes or other bursts count too, so a packed crowd can go off in a chain.
 * Combinations that change the death burst:
 * <ul>
 * <li>{@link Synergy#FROST_BURST}: the burst chills what it catches.</li>
 * <li>{@link Synergy#CYCLONE}: the death draws the enemies around it in before the burst goes off.</li>
 * <li>{@link Synergy#HURRICANE}: while rampaging, the burst is half again as wide.</li>
 * <li>{@link Synergy#TOXIC_GUST}: the burst poisons what it catches.</li>
 * <li>{@link Synergy#HEXWIND}: the burst may curse what it catches.</li>
 * <li>{@link Synergy#ASHEN_WIND}: the burst also releases a Withering soul burst.</li>
 * <li>{@link Synergy#BULWARK_GUST}: the burst throws enemies away from the player.</li>
 * <li>{@link Synergy#SPARK_BURST}: every enemy it pushes zaps one more enemy with Copper Shock.</li>
 * </ul>
 */
final class WindCharged {
	private static final double RADIUS = 2.5;
	/** Burst damage for levels 1 to 3. */
	private static final float[] DAMAGE = {3.0F, 4.0F, 5.0F};
	/** Cyclone: how far the pull before the burst reaches, and how long the burst waits for it. */
	private static final double CYCLONE_RADIUS = 5.0;
	private static final int CYCLONE_DELAY = 6;
	/** Hurricane: how much wider the burst is while rampaging. */
	private static final double HURRICANE = 1.5;
	/** Toxic Gust: how long the poison lasts, 1.5 s. */
	private static final int TOXIC_GUST_TICKS = 30;
	/** Hexwind: chance each enemy caught is cursed. */
	private static final float HEXWIND_CHANCE = 0.4F;

	private WindCharged() {
	}

	static void onKill(ServerPlayer player, ItemStack weapon, LivingEntity victim) {
		int level = Perks.level(weapon, Perks.WIND_CHARGED);
		if (level <= 0) {
			return;
		}
		Adaptation.used(player, weapon, Perks.WIND_CHARGED);
		float damage = DAMAGE[Math.min(level, DAMAGE.length) - 1];
		int cold = Synergy.FROST_BURST.on(weapon) ? Perks.level(weapon, Perks.COLD_EDGE) : 0;
		boolean toxic = Synergy.TOXIC_GUST.on(weapon);
		int curse = Synergy.HEXWIND.on(weapon) ? Perks.level(weapon, Perks.CURSE) : 0;
		int withering = Synergy.ASHEN_WIND.on(weapon) ? Perks.level(weapon, Perks.WITHERING) : 0;
		boolean spark = Synergy.SPARK_BURST.on(weapon);
		boolean bulwark = Synergy.BULWARK_GUST.on(weapon);
		boolean hurricane = Synergy.HURRICANE.on(weapon);
		ItemStack held = weapon.copy();
		Vec3 at = victim.position().add(0.0, victim.getBbHeight() * 0.5, 0.0);
		int gravity = Synergy.CYCLONE.on(weapon) ? Perks.level(weapon, Perks.GRAVITY) : 0;
		if (gravity > 0) {
			// Cyclone: the death draws the crowd in first, and the burst waits for it to arrive.
			Gravity.pull(player, victim.position(), CYCLONE_RADIUS, Gravity.strength(gravity), e -> false, true);
		}
		// At least a tick later, so a burst that kills does not die again inside this death, and a
		// rampage this kill starts already counts for Hurricane.
		CombatHooks.later(gravity > 0 ? CYCLONE_DELAY : 1, () -> {
			if (player.isRemoved() || player.level() != victim.level()) {
				return;
			}
			double radius = hurricane && Rampaging.active(player) ? RADIUS * HURRICANE : RADIUS;
			WindBurst.at(player, at, radius, damage, null, caught -> {
				if (cold > 0) {
					ColdEdge.chill(caught, cold);
				}
				if (toxic) {
					Venomous.poison(player, caught, TOXIC_GUST_TICKS);
				}
				if (curse > 0 && caught.isAlive() && player.getRandom().nextFloat() < HEXWIND_CHANCE) {
					Curse.apply(player, held, caught, curse);
				}
				if (spark && !Adaptation.resists(caught, Perks.WIND_CHARGED)) {
					CopperShock.zap(player, held, caught);
				}
			}, bulwark);
			if (withering > 0) {
				Withering.burst(player, at, withering);
			}
		});
	}
}
