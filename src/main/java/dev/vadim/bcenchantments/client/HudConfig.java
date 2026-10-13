package dev.vadim.bcenchantments.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Util;

/**
 * Client HUD settings in {@code config/bcenchantments-client.json}, editable from Mod Menu. The file is
 * written with the defaults on first launch and read again whenever it changes, so hand edits show up in
 * game without a restart.
 */
public final class HudConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("bcenchantments-client.json");
	private static final long CHECK_MS = 1000;

	/** Where the Lightning and Shockwave bars sit. */
	public enum ChargeLayout {
		/** Centred under the crosshair, the main hand's rows above the off hand's. */
		UNDER_CROSSHAIR,
		/** Beside the crosshair: the main hand's weapon on the main arm's side, the off hand's on the other. */
		BESIDE_CROSSHAIR
	}

	/** Settings as stored in the file. */
	public static final class Values {
		public ChargeLayout chargeBarLayout = ChargeLayout.UNDER_CROSSHAIR;
		/** Distance of the charge bars below the crosshair's centre, in GUI pixels, when they sit under it. */
		public float chargeBarY = 8.5F;
		/** Gap between the crosshair's centre and the nearest block, in GUI pixels, when the bars sit beside it. */
		public float chargeBarSideX = 9.0F;
		/** Vertical offset of the side bars from the crosshair's centre, in GUI pixels. */
		public float chargeBarSideY = 0.0F;
		/** Size of the charge bars; 1.0 makes each block 4 by 1 GUI pixels. */
		public float chargeBarScale = 0.75F;
		/** Off turns Better Combat's attacks off entirely, for farms that need vanilla sweeping. */
		public boolean betterCombat = true;
		/** Thins every particle this mod spawns down to a little of each effect. */
		public boolean minimalParticles = false;

		public Values copy() {
			Values copy = new Values();
			copy.chargeBarLayout = chargeBarLayout;
			copy.chargeBarY = chargeBarY;
			copy.chargeBarSideX = chargeBarSideX;
			copy.chargeBarSideY = chargeBarSideY;
			copy.chargeBarScale = chargeBarScale;
			copy.betterCombat = betterCombat;
			copy.minimalParticles = minimalParticles;
			return copy;
		}
	}

	private static Values values = new Values();
	private static long modified = Long.MIN_VALUE;
	private static long checkedAt = Long.MIN_VALUE;

	private HudConfig() {
	}

	public static Values get() {
		long now = Util.getMillis();
		if (now - checkedAt >= CHECK_MS) {
			checkedAt = now;
			reload();
		}
		return values;
	}

	/** Takes the settings screen's values and writes them. */
	public static void save(Values edited) {
		values = edited.copy();
		save();
	}

	private static void save() {
		try (Writer writer = Files.newBufferedWriter(FILE)) {
			GSON.toJson(values, writer);
		} catch (IOException e) {
			return;
		}
		try {
			modified = Files.getLastModifiedTime(FILE).toMillis();
		} catch (IOException e) {
			// The next check reads the file back, which is harmless.
		}
	}

	private static void reload() {
		try {
			if (!Files.exists(FILE)) {
				values = new Values();
				try (Writer writer = Files.newBufferedWriter(FILE)) {
					GSON.toJson(values, writer);
				}
				modified = Files.getLastModifiedTime(FILE).toMillis();
				return;
			}
			long time = Files.getLastModifiedTime(FILE).toMillis();
			if (time == modified) {
				return;
			}
			modified = time;
			try (Reader reader = Files.newBufferedReader(FILE)) {
				Values read = GSON.fromJson(reader, Values.class);
				values = read != null ? read : new Values();
				if (values.chargeBarLayout == null) {
					values.chargeBarLayout = ChargeLayout.UNDER_CROSSHAIR;
				}
			}
		} catch (IOException | JsonParseException e) {
			// A half-written or broken file keeps the last good settings.
		}
	}
}
