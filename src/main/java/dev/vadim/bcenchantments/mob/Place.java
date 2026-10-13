package dev.vadim.bcenchantments.mob;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.storage.LevelData;

/**
 * How dangerous a spawn spot is. Every monster bonus is multiplied by it: the surface near world
 * spawn stays mild, caves and the Deep Dark are harder, the Nether and End harder still.
 */
final class Place {
	private Place() {
	}

	static double multiplier(ServerLevel world, BlockPos pos) {
		if (world.dimension() == Level.END) {
			return MobBalance.PLACE_END;
		}
		if (world.dimension() == Level.NETHER) {
			return MobBalance.PLACE_NETHER;
		}
		if (world.dimension() != Level.OVERWORLD) {
			return 1.0;
		}
		if (world.getBiome(pos).is(Biomes.DEEP_DARK)) {
			return MobBalance.PLACE_DEEP_DARK;
		}
		if (pos.getY() < world.getSeaLevel() && !world.canSeeSky(pos)) {
			return MobBalance.PLACE_UNDERGROUND;
		}
		LevelData.RespawnData spawn = world.getRespawnData();
		if (spawn.dimension() == world.dimension()
			&& spawn.pos().distSqr(new BlockPos(pos.getX(), spawn.pos().getY(), pos.getZ())) < MobBalance.SPAWN_RADIUS * MobBalance.SPAWN_RADIUS) {
			return MobBalance.PLACE_NEAR_SPAWN;
		}
		return 1.0;
	}
}
