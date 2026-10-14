package dev.vadim.bcenchantments.lab;

import dev.vadim.bcenchantments.ui.Json;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Minecraft's default font: the bitmap providers from the jar, laid out the way the game lays them out. */
final class McFont {
	private record Glyph(BufferedImage image, int width, int advance, int ascent, float scale) {
	}

	private final Assets assets;
	private final Map<Integer, Glyph> glyphs = new HashMap<>();
	private final Map<Integer, Integer> spaces = new HashMap<>();
	private final Map<Long, BufferedImage> tinted = new HashMap<>();

	McFont(Assets assets) {
		this.assets = assets;
		load("font/default.json");
	}

	@SuppressWarnings("unchecked")
	private void load(String path) {
		String json = assets.text(path);
		if (json == null) {
			return;
		}
		for (Object p : (List<Object>) Json.object(json).get("providers")) {
			Map<String, Object> provider = (Map<String, Object>) p;
			switch ((String) provider.get("type")) {
				case "reference" -> {
					Map<String, Object> filter = (Map<String, Object>) provider.get("filter");
					if (filter == null || !Boolean.TRUE.equals(filter.get("uniform"))) {
						load("font/" + ((String) provider.get("id")).replace("minecraft:", "") + ".json");
					}
				}
				case "space" -> ((Map<String, Object>) provider.get("advances")).forEach(
					(key, value) -> spaces.putIfAbsent(key.codePointAt(0), ((Double) value).intValue()));
				case "bitmap" -> bitmap(provider);
				default -> {
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void bitmap(Map<String, Object> provider) {
		String file = ((String) provider.get("file")).replace("minecraft:", "textures/");
		BufferedImage atlas = assets.image(file);
		if (atlas == null) {
			return;
		}
		List<String> rows = (List<String>) provider.get("chars");
		int ascent = ((Double) provider.get("ascent")).intValue();
		int cols = rows.getFirst().codePointCount(0, rows.getFirst().length());
		int cw = atlas.getWidth() / cols;
		int ch = atlas.getHeight() / rows.size();
		float height = provider.containsKey("height") ? ((Double) provider.get("height")).floatValue() : 8.0F;
		float scale = height / ch;
		for (int r = 0; r < rows.size(); r++) {
			int[] points = rows.get(r).codePoints().toArray();
			for (int c = 0; c < points.length; c++) {
				int cp = points[c];
				if (cp == 0 || glyphs.containsKey(cp) || spaces.containsKey(cp)) {
					continue;
				}
				int width = 0;
				for (int x = cw - 1; x >= 0 && width == 0; x--) {
					for (int y = 0; y < ch; y++) {
						if ((atlas.getRGB(c * cw + x, r * ch + y) >>> 24) != 0) {
							width = x + 1;
							break;
						}
					}
				}
				BufferedImage cell = atlas.getSubimage(c * cw, r * ch, Math.max(1, width), ch);
				glyphs.put(cp, new Glyph(cell, width, (int) (0.5F + width * scale) + 1, ascent, scale));
			}
		}
	}

	private int advance(int cp) {
		Integer space = spaces.get(cp);
		if (space != null) {
			return space;
		}
		Glyph g = glyphs.get(cp);
		return g != null ? g.advance : glyphs.containsKey((int) '?') ? glyphs.get((int) '?').advance : 4;
	}

	int width(String text) {
		int w = 0;
		for (int i = 0; i < text.length(); ) {
			int cp = text.codePointAt(i);
			i += Character.charCount(cp);
			w += advance(cp);
		}
		return w;
	}

	private BufferedImage tint(int cp, Glyph g, int argb) {
		return tinted.computeIfAbsent((long) cp << 32 | (argb & 0xFFFFFFFFL), key -> {
			BufferedImage out = new BufferedImage(g.image.getWidth(), g.image.getHeight(), BufferedImage.TYPE_INT_ARGB);
			int alpha = argb >>> 24;
			for (int y = 0; y < out.getHeight(); y++) {
				for (int x = 0; x < out.getWidth(); x++) {
					int a = (g.image.getRGB(x, y) >>> 24) * alpha / 255;
					out.setRGB(x, y, a << 24 | argb & 0xFFFFFF);
				}
			}
			return out;
		});
	}

	/** Draws one run at ({@code x}, {@code y}), the top of the line, in GUI pixels. */
	void draw(Graphics2D g, String text, int x, int y, int argb, boolean shadow) {
		if (shadow) {
			int dark = argb & 0xFF000000 | (int) ((argb >> 16 & 0xFF) * 0.25F) << 16 | (int) ((argb >> 8 & 0xFF) * 0.25F) << 8 | (int) ((argb & 0xFF) * 0.25F);
			run(g, text, x + 1, y + 1, dark);
		}
		run(g, text, x, y, argb);
	}

	private void run(Graphics2D g, String text, int x, int y, int argb) {
		int px = x;
		for (int i = 0; i < text.length(); ) {
			int cp = text.codePointAt(i);
			i += Character.charCount(cp);
			Glyph glyph = glyphs.get(cp);
			if (glyph != null && glyph.width > 0 && !spaces.containsKey(cp)) {
				BufferedImage image = tint(cp, glyph, argb);
				int top = y + 7 - glyph.ascent;
				g.drawImage(image, px, top, Math.round(image.getWidth() * glyph.scale), Math.round(image.getHeight() * glyph.scale), null);
			}
			px += advance(cp);
		}
	}
}
