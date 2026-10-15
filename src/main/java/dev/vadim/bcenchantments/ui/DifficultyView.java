package dev.vadim.bcenchantments.ui;

import java.util.List;

/**
 * The difficulty panel above the inventory: centred on it, as tall as its rows up to the room above
 * the inventory, scrolling past that. Shared by the mod and the UI lab.
 */
public final class DifficultyView {
	private DifficultyView() {
	}

	/**
	 * Draws the panel over a container {@code imageWidth} wide whose top left is ({@code left},
	 * {@code top}). Returns the view {x0, y0, x1, y1} the rows scroll in, or null when there is no room
	 * above the container; afterwards {@link Ctx#hovered} is the item to give a tooltip, or null.
	 * {@code bounds} receives the panel's own edges.
	 */
	public static int[] draw(Ctx c, List<Row> rows, int screenWidth, int left, int top, int imageWidth, float alpha,
		SmithingView.Scroll scroll, int[] bounds) {
		c.view = "difficulty";
		rows = Layout.apply(c.sheet, c.view, rows);
		c.rows = rows;
		int gap = c.num("difficulty", "gap", 4);
		int pad = c.num("difficulty", "padding", 5);
		int panelWidth = Math.min(c.num("difficulty", "width", 220), screenWidth - 2 * gap);
		int x0 = left + (imageWidth - panelWidth) / 2;
		x0 = Math.max(gap, Math.min(x0, screenWidth - gap - panelWidth));
		int x1 = x0 + panelWidth;
		int width = panelWidth - 2 * pad - c.num("difficulty", "scrollbar_room", 4);
		int total = 0;
		for (Row row : rows) {
			total += SmithingView.rowHeight(c, row, width);
		}
		int y1 = top - gap;
		int y0 = Math.max(gap, y1 - total - 2 * pad);
		if (y1 - y0 < c.num("difficulty", "min_height", 2 * pad + c.line())) {
			return null;
		}
		bounds[0] = x0;
		bounds[1] = y0;
		bounds[2] = x1;
		bounds[3] = y1;
		c.alpha = alpha;
		c.box("difficulty.box", new Ctx.BoxDef("$PANEL", 0.85F, "$BORDER"), false, x0, y0, x1, y1);
		int vx0 = x0 + pad;
		int vy0 = y0 + pad;
		int vx1 = x1 - pad;
		int vy1 = y1 - pad;
		scroll.max = Math.max(0, total - (vy1 - vy0));
		scroll.value = Math.max(0.0F, Math.min(scroll.value, scroll.max));

		c.scissor(vx0, vy0, vx1, vy1);
		SmithingView.column(c, rows, vx0, vy0 - Math.round(scroll.value), width, vy0, vy1, 0);
		c.unscissor();

		if (scroll.max > 0) {
			int track = vy1 - vy0;
			int thumb = Math.max(8, track * track / (track + scroll.max));
			int ty = vy0 + Math.round((track - thumb) * scroll.value / scroll.max);
			c.fill("scroll.track", vx1 - 2, vy0, vx1, vy1, new Ctx.Fill("$BORDER", 0.5F));
			c.fill("scroll.thumb", vx1 - 2, ty, vx1, ty + thumb, new Ctx.Fill("$BORDER_HOVER", 1.0F));
		}
		if (!c.over(vx0, vy0, vx1, vy1)) {
			c.hovered = null;
		}
		return new int[] {vx0, vy0, vx1, vy1};
	}
}
