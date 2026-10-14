package dev.vadim.bcenchantments.ui;

import java.util.List;
import java.util.function.IntConsumer;

/** The Field Guide's window: title, tabs and a scrolling column of blocks. Shared by the mod and the UI lab. */
public final class GuideView {
	private static final Ctx.BoxDef TAB = new Ctx.BoxDef("$SUNKEN", 0.85F, "$BORDER_CONTROL", "$SUNKEN", 0.85F, "$BORDER_HOVER");
	private static final Ctx.BoxDef TAB_SELECTED = new Ctx.BoxDef("$ACCENT", 1.0F, "$TEXT_MUTED");

	private GuideView() {
	}

	/**
	 * Draws the guide on a screen {@code screenWidth} by {@code screenHeight}. {@code alpha} is the enter
	 * animation, {@code fade} and {@code rise} (0 to 1) how far the tab's blocks have arrived after a switch.
	 * Returns the view {x0, y0, x1, y1} the blocks scroll in.
	 */
	public static int[] draw(Ctx c, int screenWidth, int screenHeight, Rich title, Rich hint, List<Rich> tabs, int selected,
		IntConsumer onTab, List<Row> rows, SmithingView.Scroll scroll, float alpha, float switched) {
		c.view = "guide." + selected;
		rows = Layout.apply(c.sheet, c.view, rows);
		c.rows = rows;
		int margin = c.num("guide", "margin", 12);
		int padding = c.num("guide", "padding", 8);
		int panelWidth = Math.min(c.num("guide", "width", 340), screenWidth - 2 * margin);
		int panelHeight = Math.min(c.num("guide", "height", 250), screenHeight - 2 * margin);
		int x0 = (screenWidth - panelWidth) / 2;
		int y0 = (screenHeight - panelHeight) / 2;
		int x1 = x0 + panelWidth;
		int y1 = y0 + panelHeight;
		c.alpha = alpha;
		c.box("guide.box", new Ctx.BoxDef("$PANEL", 0.85F, "$BORDER"), false, x0, y0, x1, y1);

		int inner0 = x0 + padding;
		int inner1 = x1 - padding;
		int y = y0 + padding;
		c.text("guide.title", title, inner0, y, Ctx.Tx.shadowed("$TEXT_BRIGHT"));
		Ctx.Tx hintStyle = Ctx.Tx.of("$TEXT_FAINT");
		c.text("guide.hint", hint, inner1 - c.width("guide.hint", hint, hintStyle), y, hintStyle);
		y += c.num("guide", "title_height", 16);

		int tabHeight = c.num("guide", "tab_height", 14);
		int tabGap = c.num("guide", "tab_gap", 2);
		int tabWidth = (inner1 - inner0 - (tabs.size() - 1) * tabGap) / tabs.size();
		for (int i = 0; i < tabs.size(); i++) {
			int tx0 = inner0 + i * (tabWidth + tabGap);
			int tx1 = i == tabs.size() - 1 ? inner1 : tx0 + tabWidth;
			int index = i;
			boolean on = i == selected;
			boolean over = c.over(tx0, y, tx1, y + tabHeight);
			c.box(on ? "tab.selected" : "tab.box", on ? TAB_SELECTED : TAB, over, tx0, y, tx1, y + tabHeight);
			Ctx.Tx style = new Ctx.Tx(on ? "$ON_ACCENT" : "$TEXT_MUTED", false, 1.0F, on ? "$ON_ACCENT" : "$TEXT_BODY");
			String part = on ? "tab.label_selected" : "tab.label";
			Rich label = tabs.get(i);
			c.text(part, label, (tx0 + tx1 - c.width(part, label, style)) / 2, y + 3, style, over, -1);
			c.hit(tx0, y, tx1, y + tabHeight, () -> onTab.accept(index));
		}
		y += tabHeight + padding;

		int vx0 = inner0;
		int vy0 = y;
		int vx1 = inner1;
		int vy1 = y1 - padding;
		int width = vx1 - vx0 - c.num("guide", "scrollbar_room", 6);
		int gap = c.num("guide", "gap", 8);
		int total = -gap;
		for (Row row : rows) {
			total += SmithingView.rowHeight(c, row, width) + gap;
		}
		scroll.max = Math.max(0, total - (vy1 - vy0));
		scroll.value = Math.max(0.0F, Math.min(scroll.value, scroll.max));

		c.alpha = alpha * switched;
		c.scissor(vx0, vy0, vx1, vy1);
		c.push();
		c.translate(0.0F, (1.0F - switched) * 6.0F);
		SmithingView.column(c, rows, vx0, vy0 - Math.round(scroll.value), width, vy0, vy1, gap);
		c.pop();
		c.unscissor();

		c.alpha = alpha;
		if (scroll.max > 0) {
			int track = vy1 - vy0;
			int thumb = Math.max(12, track * track / (track + scroll.max));
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
