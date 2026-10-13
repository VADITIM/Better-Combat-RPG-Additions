package dev.vadim.bcenchantments.mob;

import net.minecraft.world.entity.MobCategory;

/** More monsters at once: raises the monster spawn cap, which is what limits natural spawning. */
public final class SpawnDensity {
	private SpawnDensity() {
	}

	public static int maxInstances(MobCategory category, int vanilla) {
		return category == MobCategory.MONSTER ? Math.round(vanilla * MobBalance.MONSTER_CAP_MULTIPLIER) : vanilla;
	}
}
