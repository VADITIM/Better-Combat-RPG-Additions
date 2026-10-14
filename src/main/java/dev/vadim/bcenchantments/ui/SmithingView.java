package dev.vadim.bcenchantments.ui;

import java.util.List;

/**
 * The window to the left of the smithing table: its size and position next to the table, and how it draws
 * a list of rows inside a scrolling frame. The mod and the UI lab both draw it through here.
 */
public final class SmithingView {
	public static final int TABLE_WIDTH = 176;
	public static final int TABLE_HEIGHT = 166;

	/** Scroll position of a view, and how far it can go (set by the last draw). */
	public static final class Scroll {
		public float value;
		public int max;
	}

	private SmithingView() {
	}

	/** How wide the panel is on a screen this wide: 0 when closed or when it does not fit beside the table. */
	public static int width(Sheet s, int screenWidth, boolean open) {
		if (!open) {
			return 0;
		}
		int column = s.num("panel.button_column", 24);
		int w = Math.min(s.num("panel.max_width", 168), screenWidth - TABLE_WIDTH - s.num("panel.gap", 4) - 2 * (column + s.num("panel.gap", 4)));
		return w < s.num("panel.min_width", 120) ? 0 : w;
	}

	/** The table's left edge with the panel open: panel and table centred together, like the recipe book. */
	public static int tableLeft(Sheet s, int screenWidth, boolean open) {
		int w = width(s, screenWidth, open);
		int gap = s.num("panel.gap", 4);
		return w == 0 ? (screenWidth - TABLE_WIDTH) / 2 : (screenWidth - w - gap - TABLE_WIDTH) / 2 + w + gap;
	}

	/** The panel's bounds {x0, y0, x1, y1}: half again the table's height, centred on it. */
	public static int[] bounds(Sheet s, int screenWidth, int screenHeight, boolean open, int left, int top) {
		int gap = s.num("panel.gap", 4);
		int x1 = left - gap;
		int x0 = x1 - width(s, screenWidth, open);
		int y0 = Math.max(gap, top - TABLE_HEIGHT / 4);
		int y1 = Math.min(screenHeight - gap, top + TABLE_HEIGHT + TABLE_HEIGHT / 4);
		return new int[] {x0, y0, x1, y1};
	}

	/** The height one row takes with the spacing the sheet gives its kind. */
	public static int rowHeight(Ctx c, Row row, int width) {
		return c.num("row." + row.kind(), "top", 0) + row.height(c, width) + c.num("row." + row.kind(), "bottom", 0);
	}

	/** Draws the rows of a column at ({@code x}, {@code y}) as far as {@code y1}, noting each row as a part. */
	public static void column(Ctx c, List<Row> rows, int x, int y, int width, int y0, int y1, int gap) {
		for (Row row : rows) {
			int top = c.num("row." + row.kind(), "top", 0);
			int h = rowHeight(c, row, width);
			if (y + h >= y0 && y <= y1) {
				String ref = row instanceof Layout.Placed p ? p.ref() : null;
				c.note("row." + row.kind(), "row", x, y + top, x + width, y + h - c.num("row." + row.kind(), "bottom", 0), row.label(), null, ref, false);
				c.row = ref;
				row.draw(c, x, y + top, width);
				c.row = null;
			}
			y += h + gap;
		}
	}

	/**
	 * Draws the panel. {@code panelAlpha} is the enter animation; {@code settle} (0 to 1) is how far the rows
	 * have settled after the weapon changed. Returns the view {x0, y0, x1, y1} the rows scroll in; afterwards
	 * {@link Ctx#hovered} is the item to give a tooltip, or null.
	 */
	public static int[] draw(Ctx c, List<Row> rows, int[] bounds, float panelAlpha, float settle, Scroll scroll) {
		int x0 = bounds[0];
		int y0 = bounds[1];
		int x1 = bounds[2];
		int y1 = bounds[3];
		c.view = "panel";
		rows = Layout.apply(c.sheet, c.view, rows);
		c.rows = rows;
		c.alpha = panelAlpha;
		c.box("panel.box", new Ctx.BoxDef("$PANEL", 0.85F, "$BORDER"), false, x0, y0, x1, y1);
		int pad = c.num("panel", "padding", 6);
		int vx0 = x0 + pad;
		int vx1 = x1 - pad;
		int vy0 = y0 + pad;
		int vy1 = y1 - pad;
		int width = vx1 - vx0 - c.num("panel", "scrollbar_room", 4);
		int total = 0;
		for (Row row : rows) {
			total += rowHeight(c, row, width);
		}
		scroll.max = Math.max(0, total - (vy1 - vy0));
		scroll.value = Math.max(0.0F, Math.min(scroll.value, scroll.max));

		c.alpha = panelAlpha * settle;
		c.scissor(vx0, vy0, vx1, vy1);
		c.push();
		c.translate(0.0F, (1.0F - settle) * 6.0F);
		column(c, rows, vx0, vy0 - Math.round(scroll.value), width, vy0, vy1, 0);
		c.pop();
		c.unscissor();

		c.alpha = panelAlpha;
		if (scroll.max > 0) {
			int track = vy1 - vy0;
			int thumb = Math.max(12, track * track / (track + scroll.max));
			int ty = vy0 + Math.round((track - thumb) * scroll.value / scroll.max);
			c.fill("scroll.track", vx1 - 2, vy0, vx1, vy1, new Ctx.Fill("$BORDER", 0.5F));
			c.fill("scroll.thumb", vx1 - 2, ty, vx1, ty + thumb, new Ctx.Fill("$BORDER_HOVER", 1.0F));
		}
		boolean inside = c.over(vx0, vy0, vx1, vy1);
		if (c.tip != null && inside) {
			c.hovered = null;
			c.alpha = 1.0F;
			Ctx.Tx style = Ctx.Tx.of("$DANGER");
			int tw = c.width("tip.text", c.tip, style);
			int tx = c.mouseX + 8;
			int ty = c.mouseY - 16;
			c.box("tip.box", new Ctx.BoxDef("$PANEL", 0.85F, "$BORDER_CONTROL"), false, tx - 4, ty - 4, tx + tw + 4, ty + c.line() + 3);
			c.text("tip.text", c.tip, tx, ty, style);
		} else if (!inside) {
			c.hovered = null;
		}
		return new int[] {vx0, vy0, vx1, vy1};
	}
}
