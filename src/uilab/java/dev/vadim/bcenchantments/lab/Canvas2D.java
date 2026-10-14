package dev.vadim.bcenchantments.lab;

import dev.vadim.bcenchantments.ui.Canvas;
import dev.vadim.bcenchantments.ui.ItemRef;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/** The shared UI code's {@link Canvas} over Java2D, with Minecraft's font and item sprites. Units are GUI pixels. */
final class Canvas2D implements Canvas {
	private final Assets assets;
	private final McFont font;
	/** Preview-only item swaps from the inspector: shown id to replacement id. */
	final Map<String, String> swaps = new HashMap<>();
	private final Map<String, BufferedImage> glints = new HashMap<>();
	private final Deque<AffineTransform> stack = new ArrayDeque<>();
	Graphics2D g;

	Canvas2D(Assets assets, McFont font) {
		this.assets = assets;
		this.font = font;
	}

	void bind(Graphics2D g) {
		this.g = g;
		stack.clear();
	}

	@Override
	public void fill(int x0, int y0, int x1, int y1, int argb) {
		if (x1 <= x0 || y1 <= y0 || argb >>> 24 == 0) {
			return;
		}
		g.setColor(new Color(argb, true));
		g.fillRect(x0, y0, x1 - x0, y1 - y0);
	}

	@Override
	public void text(String text, int x, int y, int argb, boolean shadow) {
		if (argb >>> 24 == 0) {
			return;
		}
		font.draw(g, text, x, y, argb, shadow);
	}

	@Override
	public int width(String text) {
		return font.width(text);
	}

	@Override
	public void item(ItemRef item, int x, int y) {
		String id = swaps.getOrDefault(item.id(), item.id());
		BufferedImage sprite = assets.item(id);
		if (sprite == null) {
			g.setColor(new Color(0x60404060, true));
			g.fillRect(x + 2, y + 2, 12, 12);
			g.setColor(new Color(0xFFB0B0D0, true));
			g.drawRect(x + 2, y + 2, 11, 11);
		} else {
			g.drawImage(item.glint() ? glint(id, sprite) : sprite, x, y, 16, 16, null);
		}
		if (item.count() > 1) {
			String count = Integer.toString(item.count());
			font.draw(g, count, x + 17 - font.width(count), y + 9, 0xFFFFFFFF, true);
		}
	}

	/** The sprite with a purple wash over its pixels, standing in for the enchantment glint. */
	private BufferedImage glint(String id, BufferedImage sprite) {
		return glints.computeIfAbsent(id, key -> {
			BufferedImage out = new BufferedImage(sprite.getWidth(), sprite.getHeight(), BufferedImage.TYPE_INT_ARGB);
			for (int y = 0; y < out.getHeight(); y++) {
				for (int x = 0; x < out.getWidth(); x++) {
					int p = sprite.getRGB(x, y);
					if (p >>> 24 == 0) {
						continue;
					}
					int r = (p >> 16 & 0xFF) * 6 / 10 + 0x80 * 4 / 10;
					int gr = (p >> 8 & 0xFF) * 6 / 10 + 0x40 * 4 / 10;
					int b = (p & 0xFF) * 6 / 10 + 0xFF * 4 / 10;
					out.setRGB(x, y, p & 0xFF000000 | Math.min(255, r) << 16 | Math.min(255, gr) << 8 | Math.min(255, b));
				}
			}
			return out;
		});
	}

	@Override
	public void push() {
		stack.push(g.getTransform());
	}

	@Override
	public void pop() {
		g.setTransform(stack.pop());
	}

	@Override
	public void translate(float x, float y) {
		g.translate(x, y);
	}

	@Override
	public void scale(float factor) {
		g.scale(factor, factor);
	}

	@Override
	public void scissor(int x0, int y0, int x1, int y1) {
		g.setClip(x0, y0, x1 - x0, y1 - y0);
	}

	@Override
	public void unscissor() {
		g.setClip(null);
	}
}
