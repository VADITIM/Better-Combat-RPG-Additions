package dev.vadim.bcenchantments.mob;

import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.storage.LevelData;
import org.jspecify.annotations.Nullable;

/**
 * How dangerous a spawn spot is. Every monster bonus is multiplied by it: the surface near world
 * spawn stays mild, caves and the Deep Dark are harder, the Nether and End harder still, and
 * structures ({@link Landmark}) have their own. Each spot also has a {@link Tier}, which picks the
 * armor monsters spawn in ({@link MobArmor}) and is what the inventory's difficulty panel shows; Bad
 * Omen raises it. Both sides can ask: the client passes the landmark the server last told it of.
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
		/** The harder of the two, taken separately for the multiplier and the tier. */
		Spot max(Spot other) {
			return new Spot(Math.max(multiplier, other.multiplier),
				tier.compareTo(other.tier) >= 0 ? tier : other.tier);
		}
	}

	private Place() {
	}

	/** The spot at {@code pos}, with {@code landmark} the structure there, if any. */
	public static Spot of(Level world, BlockPos pos, @Nullable Landmark landmark) {
		Spot surroundings = surroundings(world, pos);
		return landmark == null ? surroundings : surroundings.max(landmark.spot());
	}

	/** The spot at {@code pos}, looking up the structure there. */
	static Spot of(ServerLevel world, BlockPos pos) {
		return of(world, pos, Landmark.at(world, pos));
	}

	/** The spot by dimension, biome, depth and distance from spawn, structures aside. */
	private static Spot surroundings(Level world, BlockPos pos) {
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

	/** The spot's tier, raised one step for Bad Omen I and II, two for III and IV, three for V. */
	public static Tier tier(Spot spot, int omen) {
		return spot.tier().raise((omen + 1) / 2);
	}
}
