package dev.vadim.bcenchantments.lab;

import dev.vadim.bcenchantments.ui.Ctx;
import dev.vadim.bcenchantments.ui.GuideView;
import dev.vadim.bcenchantments.ui.ItemRef;
import dev.vadim.bcenchantments.ui.Rich;
import dev.vadim.bcenchantments.ui.Sheet;
import dev.vadim.bcenchantments.ui.SmithingView;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * The fake game screen: Minecraft's dimmed background, the smithing table with its two buttons, or the
 * Field Guide, drawn through the same shared code as the mod. Holds the state a click changes.
 */
final class Stage {
	static final int TABLE_SLOT_Y = 48;

	final Assets assets;
	final McFont font;
	final Canvas2D canvas;
	final Lang lang;
	final Sheet sheet;
	final Scenarios scenarios;
	final Ctx ctx;
	final Scenarios.State state = new Scenarios.State();
	final List<Scenarios.Smithing> tables;

	/** The view on show: a smithing setup by index, or -1 for the Field Guide. */
	int view;
	boolean panelOpen = true;
	boolean showTable = true;
	long enterAt;
	long changedAt;
	long tabAt;
	final SmithingView.Scroll scroll = new SmithingView.Scroll();
	final SmithingView.Scroll[] guideScroll = {new SmithingView.Scroll(), new SmithingView.Scroll(), new SmithingView.Scroll(), new SmithingView.Scroll()};
	/** Where the rows scroll, {x0, y0, x1, y1}, after the last frame. */
	int[] rowsView;
	/** The panel's bounds after the last frame, or null when it is closed. */
	int[] panelBounds;
	int guiWidth;
	int guiHeight;
	int tableLeft;
	int tableTop;

	Stage(Assets assets, McFont font, Lang lang, Sheet sheet) {
		this.assets = assets;
		this.font = font;
		this.lang = lang;
		this.sheet = sheet;
		this.canvas = new Canvas2D(assets, font);
		this.scenarios = new Scenarios(lang);
		this.tables = scenarios.smithing();
		this.ctx = new Ctx(canvas, sheet);
		this.ctx.record = true;
		this.view = 2;
	}

	boolean guide() {
		return view < 0;
	}

	/** The view on show as {@link dev.vadim.bcenchantments.ui.Layout} names it: {@code panel} or {@code guide.<tab>}. */
	String viewName() {
		return guide() ? "guide." + state.tab : "panel";
	}

	void replay(long now) {
		enterAt = now;
		changedAt = now;
		tabAt = now;
	}

	/** Shows smithing setup {@code index}, or Field Guide tab {@code index - setups} from there on. */
	void select(int index, long now) {
		if (index >= tables.size()) {
			if (!guide()) {
				view = -1 - view;
				replay(now);
			}
			int tab = Math.min(3, index - tables.size());
			if (tab != state.tab) {
				state.tab = tab;
				tabAt = now;
			}
			return;
		}
		if (guide()) {
			view = -1 - view;
		}
		if (index != view) {
			view = index;
			scroll.value = 0;
			state.expanded = null;
		}
		replay(now);
	}

	/** The index {@link #select} would take to show what is on screen now. */
	int selected() {
		return guide() ? tables.size() + state.tab : view;
	}

	private static float easeOut(float t) {
		float inverse = 1.0F - Math.max(0.0F, Math.min(1.0F, t));
		return 1.0F - inverse * inverse * inverse;
	}

	/** Draws one frame of {@code w} by {@code h} device pixels at GUI scale {@code scale}; the mouse is in GUI pixels. */
	void render(Graphics2D g, int scale, int w, int h, int mx, int my, long now) {
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
		g.setClip(0, 0, w, h);
		g.scale(scale, scale);
		guiWidth = Math.max(1, w / scale);
		guiHeight = Math.max(1, h / scale);
		canvas.bind(g);
		backdrop(g);
		ctx.begin(mx, my);
		panelBounds = null;
		rowsView = null;
		if (guide()) {
			float alpha = easeOut((now - enterAt) / 350.0F);
			float switched = easeOut((now - tabAt) / 250.0F);
			rowsView = GuideView.draw(ctx, guiWidth, guiHeight, scenarios.guideTitle(), scenarios.guideHint(), scenarios.tabLabels(), state.tab, this::selectTab,
				scenarios.guide(state.tab), guideScroll[state.tab], alpha, switched);
		} else {
			table(g, mx, my, now);
		}
		if (ctx.hovered != null) {
			tooltip(g, ctx.hovered, mx, my);
		}
	}

	private void selectTab(int tab) {
		if (tab != state.tab) {
			state.tab = tab;
			tabAt = System.currentTimeMillis();
		}
	}

	private void backdrop(Graphics2D g) {
		BufferedImage dirt = assets.image("textures/block/dirt.png");
		if (dirt != null) {
			for (int y = 0; y < guiHeight; y += 32) {
				for (int x = 0; x < guiWidth; x += 32) {
					g.drawImage(dirt, x, y, 32, 32, null);
				}
			}
		} else {
			g.setColor(new Color(0x6B5438));
			g.fillRect(0, 0, guiWidth, guiHeight);
		}
		g.setColor(new Color(0xC8101010, true));
		g.fillRect(0, 0, guiWidth, guiHeight);
	}

	private void table(Graphics2D g, int mx, int my, long now) {
		Scenarios.Smithing sc = tables.get(view);
		tableLeft = SmithingView.tableLeft(sheet, guiWidth, panelOpen);
		tableTop = (guiHeight - SmithingView.TABLE_HEIGHT) / 2;
		if (showTable) {
			BufferedImage texture = assets.image("textures/gui/container/smithing.png");
			if (texture != null) {
				g.drawImage(texture.getSubimage(0, 0, SmithingView.TABLE_WIDTH, SmithingView.TABLE_HEIGHT), tableLeft, tableTop, null);
			}
			slotItem(sc.template(), 8, TABLE_SLOT_Y);
			slotItem(sc.base(), 26, TABLE_SLOT_Y);
			slotItem(sc.addition(), 44, TABLE_SLOT_Y);
			slotItem(sc.result(), 98, TABLE_SLOT_Y);
			Rich title = Rich.of(lang.raw("container.upgrade"));
			font.draw(g, title.plain(), tableLeft + 44, tableTop + 15, 0xFF404040, false);
			int bx = tableLeft + SmithingView.TABLE_WIDTH + 4;
			button(g, bx, tableTop, panelOpen ? "<" : ">", mx, my);
			button(g, bx, tableTop + 24, "?", mx, my);
		}
		int width = SmithingView.width(sheet, guiWidth, panelOpen);
		if (width > 0) {
			panelBounds = SmithingView.bounds(sheet, guiWidth, guiHeight, panelOpen, tableLeft, tableTop);
			float panelAlpha = easeOut((now - enterAt) / 350.0F);
			float settle = easeOut((now - changedAt) / 250.0F);
			rowsView = SmithingView.draw(ctx, sc.rows().apply(state), panelBounds, panelAlpha, settle, scroll);
		}
	}

	private void slotItem(ItemRef item, int x, int y) {
		if (item != null && !item.empty()) {
			canvas.item(item, tableLeft + x, tableTop + y);
		}
	}

	private void button(Graphics2D g, int x, int y, String label, int mx, int my) {
		boolean over = mx >= x && mx < x + 20 && my >= y && my < y + 20;
		BufferedImage sprite = assets.image("textures/gui/sprites/widget/" + (over ? "button_highlighted" : "button") + ".png");
		if (sprite != null) {
			int w = sprite.getWidth();
			int b = 3;
			g.drawImage(sprite.getSubimage(0, 0, b, b), x, y, null);
			g.drawImage(sprite.getSubimage(w - b, 0, b, b), x + 20 - b, y, null);
			g.drawImage(sprite.getSubimage(0, 20 - b, b, b), x, y + 20 - b, null);
			g.drawImage(sprite.getSubimage(w - b, 20 - b, b, b), x + 20 - b, y + 20 - b, null);
			g.drawImage(sprite.getSubimage(b, 0, w - 2 * b, b), x + b, y, 20 - 2 * b, b, null);
			g.drawImage(sprite.getSubimage(b, 20 - b, w - 2 * b, b), x + b, y + 20 - b, 20 - 2 * b, b, null);
			g.drawImage(sprite.getSubimage(0, b, b, 20 - 2 * b), x, y + b, b, 20 - 2 * b, null);
			g.drawImage(sprite.getSubimage(w - b, b, b, 20 - 2 * b), x + 20 - b, y + b, b, 20 - 2 * b, null);
			g.drawImage(sprite.getSubimage(b, b, w - 2 * b, 20 - 2 * b), x + b, y + b, 20 - 2 * b, 20 - 2 * b, null);
		} else {
			g.setColor(Color.GRAY);
			g.fillRect(x, y, 20, 20);
		}
		font.draw(g, label, x + (20 - font.width(label)) / 2, y + 6, over ? 0xFFFFFFA0 : 0xFFFFFFFF, true);
	}

	/** A plain item tooltip: the name in white on the dark purple-bordered box. */
	private void tooltip(Graphics2D g, ItemRef item, int mx, int my) {
		String id = canvas.swaps.getOrDefault(item.id(), item.id());
		String name = lang.raw("item.minecraft." + id);
		int w = font.width(name);
		int x = mx + 12;
		int y = my - 12;
		g.setColor(new Color(0xF0100010, true));
		g.fillRect(x - 3, y - 4, w + 6, 16);
		g.setColor(new Color(0xA0280070, true));
		g.drawRect(x - 3, y - 4, w + 5, 15);
		font.draw(g, name, x, y, 0xFFFFFFFF, true);
	}

	// ---- Interaction, in GUI pixels ----

	/** What a click at ({@code x}, {@code y}) does, as the game would do it; true if it hit something. */
	boolean click(int x, int y, long now) {
		if (!guide() && showTable) {
			int bx = tableLeft + SmithingView.TABLE_WIDTH + 4;
			if (x >= bx && x < bx + 20 && y >= tableTop && y < tableTop + 20) {
				panelOpen = !panelOpen;
				enterAt = now;
				changedAt = now;
				return true;
			}
			if (x >= bx && x < bx + 20 && y >= tableTop + 24 && y < tableTop + 44) {
				view = -1 - view;
				enterAt = now;
				tabAt = now;
				return true;
			}
		}
		int[] v = rowsView;
		if (!guide() && (v == null || x < v[0] || x >= v[2] || y < v[1] || y >= v[3])) {
			return false;
		}
		for (Ctx.Hit hit : new ArrayList<>(ctx.hits)) {
			if (hit.contains(x, y)) {
				hit.action().run();
				return true;
			}
		}
		return false;
	}

	/** Leaves the Field Guide for the table it was opened from. */
	void closeGuide(long now) {
		if (guide()) {
			view = -1 - view;
			enterAt = now;
		}
	}

	void scroll(int x, int y, double amount) {
		SmithingView.Scroll s = guide() ? guideScroll[state.tab] : scroll;
		boolean inside = guide() || panelBounds != null && x >= panelBounds[0] && x < panelBounds[2] && y >= panelBounds[1] && y < panelBounds[3];
		if (inside) {
			s.value = Math.max(0.0F, Math.min(s.max, s.value - (float) amount * 20));
		}
	}
}
