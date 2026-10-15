package dev.vadim.bcenchantments.mob;

import dev.vadim.bcenchantments.TrimEnchantments;
import dev.vadim.bcenchantments.network.LandmarkPayload;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.jspecify.annotations.Nullable;

/**
 * Structures with a difficulty of their own. Each is a structure tag,
 * {@code bcenchantments:difficulty/<name>}, so a datapack can add modded structures; inside one of
 * its pieces, {@link Place} uses the landmark's multiplier and tier unless the surroundings are
 * harder. They climb the way a playthrough does, so each asks for better gear than the last.
 * Villages and trial chambers are left out: trial chambers already scale with ominous bottles.
 * Only the server knows structures, so it tells each player which one they stand in, for the inventory's difficulty panel.
 */
public enum Landmark {
	// Hardest first, so where two overlap (a mineshaft through a stronghold) the harder one counts.
	END_CITY(MobBalance.LANDMARK_END_CITY, Place.Tier.EXTREME),
	ANCIENT_CITY(MobBalance.LANDMARK_ANCIENT_CITY, Place.Tier.EXTREME),
	BASTION(MobBalance.LANDMARK_BASTION, Place.Tier.EXTREME),
	MANSION(MobBalance.LANDMARK_MANSION, Place.Tier.EXTREME),
	FORTRESS(MobBalance.LANDMARK_FORTRESS, Place.Tier.VERY_HARD),
	STRONGHOLD(MobBalance.LANDMARK_STRONGHOLD, Place.Tier.VERY_HARD),
	MONUMENT(MobBalance.LANDMARK_MONUMENT, Place.Tier.VERY_HARD),
	OUTPOST(MobBalance.LANDMARK_OUTPOST, Place.Tier.HARD),
	MINESHAFT(MobBalance.LANDMARK_MINESHAFT, Place.Tier.HARD),
	TEMPLE(MobBalance.LANDMARK_TEMPLE, Place.Tier.HARD),
	RUINS(MobBalance.LANDMARK_RUINS, Place.Tier.REGULAR);

	private static final Landmark[] ALL = values();
	/** The landmark last sent to each player, as its ordinal or -1. */
	private static final Map<UUID, Integer> SENT = new HashMap<>();

	private final TagKey<Structure> structures;
	private final Place.Spot spot;

	Landmark(double multiplier, Place.Tier tier) {
		this.structures = TagKey.create(Registries.STRUCTURE, TrimEnchantments.id("difficulty/" + name().toLowerCase(Locale.ROOT)));
		this.spot = new Place.Spot(multiplier, tier);
	}

	public Place.Spot spot() {
		return spot;
	}

	public String translationKey() {
		return "landmark.bcenchantments." + name().toLowerCase(Locale.ROOT);
	}

	/** The landmark with {@code ordinal}, or null for -1 or one this build lacks. */
	public static @Nullable Landmark byOrdinal(int ordinal) {
		return ordinal >= 0 && ordinal < ALL.length ? ALL[ordinal] : null;
	}

	/** The hardest landmark with a piece at {@code pos}, or null. */
	public static @Nullable Landmark at(ServerLevel world, BlockPos pos) {
		for (Landmark landmark : ALL) {
			if (world.structureManager().getStructureWithPieceAt(pos, landmark.structures).isValid()) {
				return landmark;
			}
		}
		return null;
	}

	static void init() {
		PayloadTypeRegistry.clientboundPlay().register(LandmarkPayload.TYPE, LandmarkPayload.CODEC);
		ServerTickEvents.END_SERVER_TICK.register(Landmark::sync);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> SENT.remove(handler.getPlayer().getUUID()));
	}

	private static void sync(MinecraftServer server) {
		if (server.getTickCount() % MobBalance.LANDMARK_SYNC_TICKS != 0) {
			return;
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (!ServerPlayNetworking.canSend(player, LandmarkPayload.TYPE)) {
				continue;
			}
			Landmark landmark = at(player.level(), player.blockPosition());
			int ordinal = landmark == null ? -1 : landmark.ordinal();
			Integer sent = SENT.put(player.getUUID(), ordinal);
			if (sent == null || sent != ordinal) {
				ServerPlayNetworking.send(player, new LandmarkPayload(ordinal));
			}
		}
	}
}
