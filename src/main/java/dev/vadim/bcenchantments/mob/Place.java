package dev.vadim.bcenchantments.mob;

import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.storage.LevelData;

/**
 * How dangerous a spawn spot is. Every monster bonus is multiplied by it: the surface near world
 * spawn stays mild, caves and the Deep Dark are harder, the Nether and End harder still. Each spot
 * also has a {@link Tier}, which picks the armor monsters spawn in ({@link MobArmor}) and is what the
 * difficulty line on the HUD shows; Bad Omen raises it. Both sides can ask, so the client reads the
 * same answer the server spawns with.
 */
public final class Place {
	/** The named difficulty of a spot, mildest first. */
	public enum Tier {
		REGULAR, HARD, VERY_HARD, EXTREME, POSSIBLE_DEATH;

		public Tier raise(int steps) {
			Tier[] tiers = values();
			return tiers[Math.min(ordinal() + Math.max(steps, 0), tiers.length - 1)];
		}

		public String translationKey() {
			return "difficulty.bcenchantments." + name().toLowerCase(Locale.ROOT);
		}
	}

	public record Spot(double multiplier, Tier tier) {
	}

	private Place() {
	}

	public static Spot of(Level world, BlockPos pos) {
		Holder<Biome> biome = world.getBiome(pos);
		if (world.dimension() == Level.END) {
			return new Spot(MobBalance.PLACE_END, Tier.EXTREME);
		}
		if (world.dimension() == Level.NETHER) {
			return biome.is(Biomes.SOUL_SAND_VALLEY) || biome.is(Biomes.BASALT_DELTAS)
				? new Spot(MobBalance.PLACE_NETHER_HARSH, Tier.VERY_HARD)
				: new Spot(MobBalance.PLACE_NETHER, Tier.HARD);
		}
		if (world.dimension() != Level.OVERWORLD) {
			return new Spot(1.0, Tier.REGULAR);
		}
		if (biome.is(Biomes.DEEP_DARK)) {
			return new Spot(MobBalance.PLACE_DEEP_DARK, Tier.VERY_HARD);
		}
		if (pos.getY() < world.getSeaLevel() && !world.canSeeSky(pos)) {
			return new Spot(MobBalance.PLACE_UNDERGROUND, Tier.HARD);
		}
		LevelData.RespawnData spawn = world.getRespawnData();
		if (spawn.dimension() == world.dimension()
			&& spawn.pos().distSqr(new BlockPos(pos.getX(), spawn.pos().getY(), pos.getZ())) < MobBalance.SPAWN_RADIUS * MobBalance.SPAWN_RADIUS) {
			return new Spot(MobBalance.PLACE_NEAR_SPAWN, Tier.REGULAR);
		}
		return new Spot(1.0, Tier.REGULAR);
	}

	static double multiplier(Level world, BlockPos pos) {
		return of(world, pos).multiplier();
	}

	/** The spot's tier, raised one step for Bad Omen I and II, two for III and IV, three for V. */
	public static Tier tier(Level world, BlockPos pos, int omen) {
		return of(world, pos).tier().raise((omen + 1) / 2);
	}
}
