package dev.vadim.bcenchantments.client;

import dev.vadim.bcenchantments.mob.Omen;
import dev.vadim.bcenchantments.mob.Place;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

/**
 * A small line in the bottom right corner naming the biome the player stands in and how hard its
 * monsters are ({@link Place.Tier}), Bad Omen included: "Nether Wastes - Hard". When either changes,
 * the line decodes into the new one, letter by letter.
 */
public final class DifficultyHud implements HudElement {
	private static final int MARGIN = 4;
	private static final long LETTER_MS = 18;
	private static final String GLYPHS = "#%&*+=?@$<>/\\";

	private String shown = "";
	private long changedAt;

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null || HudSettingsScreen.isOpen(mc)) {
			return;
		}
		Place.Tier tier = Place.tier(mc.level, mc.player.blockPosition(), Omen.level(mc.player));
		Component biome = mc.level.getBiome(mc.player.blockPosition()).unwrapKey()
			.<Component>map(key -> Component.translatable(Util.makeDescriptionId("biome", key.identifier())))
			.orElse(Component.empty());
		String line = biome.getString() + " - " + Component.translatable(tier.translationKey()).getString();
		long now = Util.getMillis();
		if (!line.equals(shown)) {
			shown = line;
			changedAt = now;
		}
		Font font = mc.font;
		int x = g.guiWidth() - MARGIN - font.width(line);
		int y = g.guiHeight() - MARGIN - font.lineHeight + 1;
		g.text(font, decode(line, now - changedAt, now), x, y, Theme.argb(0.9F, Theme.TEXT_MUTED), true);
	}

	/** {@code line} with the letters not yet arrived swapped for random glyphs, left to right. */
	private static String decode(String line, long since, long now) {
		StringBuilder text = new StringBuilder(line.length());
		for (int i = 0; i < line.length(); i++) {
			char letter = line.charAt(i);
			if (letter == ' ' || since >= i * LETTER_MS) {
				text.append(letter);
			} else {
				text.append(GLYPHS.charAt(Math.floorMod(now / 40L * 31L + i * 13L, GLYPHS.length())));
			}
		}
		return text.toString();
	}
}
