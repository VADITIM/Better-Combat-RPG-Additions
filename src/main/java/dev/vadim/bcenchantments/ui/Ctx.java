package dev.vadim.bcenchantments.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * One frame of drawing: the canvas, the style sheet, the mouse, and what the frame turned up (the clickable
 * areas, the item under the mouse, and, for the UI lab, every part drawn with its bounds). All drawing of
 * the smithing panel and the Field Guide goes through here, so a part's colour, offset and size come from
 * the {@link Sheet} by its id.
 */
public final class Ctx {
	/** A style for a flat fill: colour and opacity, both overridable in the sheet. */
	public record Fill(String color, float opacity) {
	}

	/** A style for a box: fill and a hairline, with a hover variant. */
	public record BoxDef(String fill, float fillOpacity, String border, String hoverFill, float hoverFillOpacity, String hoverBorder) {
		public BoxDef(String fill, float fillOpacity, String border) {
			this(fill, fillOpacity, border, fill, fillOpacity, border);
		}
	}

	/** A style for a run of text. */
	public record Tx(String color, boolean shadow, float scale, String hoverColor) {
		public static Tx of(String color) {
			return new Tx(color, false, 1.0F, color);
		}

		public static Tx shadowed(String color) {
			return new Tx(color, true, 1.0F, color);
		}

		public Tx hover(String hoverColor) {
			return new Tx(color, shadow, scale, hoverColor);
		}

		public Tx scaled(float scale) {
			return new Tx(color, shadow, scale, hoverColor);
		}
	}

	/** A clickable area, in screen coordinates. */
	public record Hit(int x0, int y0, int x1, int y1, Runnable action) {
		public boolean contains(double x, double y) {
			return x >= x0 && x < x1 && y >= y0 && y < y1;
		}
	}

	/**
	 * Something drawn this frame, in screen coordinates and clipped to {@code clip} (or not, if null).
	 * {@code ref} is the row it belongs to ({@code view/anchor}, see {@link Layout}), or for a row its own; a
	 * {@code removed} part was not drawn.
	 */
	public record Part(String id, String kind, float x0, float y0, float x1, float y1, int[] clip, Rich text, ItemRef item, String ref, boolean removed) {
		public boolean contains(double x, double y) {
			if (clip != null && (x < clip[0] || x >= clip[2] || y < clip[1] || y >= clip[3])) {
				return false;
			}
			return x >= x0 && x < x1 && y >= y0 && y < y1;
		}
	}

	public final Canvas canvas;
	public final Sheet sheet;
	/** Multiplies the opacity of everything drawn, for the enter animation. */
	public float alpha = 1.0F;
	public int mouseX;
	public int mouseY;
	public final List<Hit> hits = new ArrayList<>();
	public final List<Part> parts = new ArrayList<>();
	/** The view being drawn, as {@link Layout} names it: {@code panel} or {@code guide.<tab>}. */
	public String view = "panel";
	/** The row being drawn ({@code view/anchor}), or null outside the rows; a part's own offset and removal in it go by it. */
	public String row;
	/** The last view's rows in the order shown, each a {@link Layout.Placed}. */
	public List<Row> rows = List.of();
	/** Whether to keep {@link #parts}; only the UI lab wants them. */
	public boolean record;
	public ItemRef hovered;
	/** A warning to show by the mouse, set by a button it is over; null for none. */
	public Rich tip;

	private float tx;
	private float ty;
	private float ts = 1.0F;
	private final float[] stack = new float[48];
	private int depth;
	private int[] clip;

	public Ctx(Canvas canvas, Sheet sheet) {
		this.canvas = canvas;
		this.sheet = sheet;
	}

	/** Starts a frame: forgets what the last one turned up. */
	public void begin(int mouseX, int mouseY) {
		this.mouseX = mouseX;
		this.mouseY = mouseY;
		hits.clear();
		parts.clear();
		hovered = null;
		tip = null;
		tx = 0;
		ty = 0;
		ts = 1.0F;
		depth = 0;
		clip = null;
		alpha = 1.0F;
		row = null;
	}

	// ---- Sheet access ----

	public int num(String part, String prop, int def) {
		return sheet.num(part + "." + prop, def);
	}

	public int line() {
		return sheet.num("ui.line", 10);
	}

	/**
	 * How far {@code part} moves along {@code axis} ({@code dx} or {@code dy}): its own offset plus, inside a
	 * row, the offset of that one part in that row ({@code part@view/anchor.dx}).
	 */
	public int offset(String part, String axis) {
		int v = num(part, axis, 0);
		if (row != null && sheet.instanced()) {
			String raw = sheet.raw(part + "@" + row + "." + axis);
			if (raw != null) {
				try {
					v += (int) Math.round(Double.parseDouble(raw));
				} catch (NumberFormatException ignored) {
					// Not a number: no offset.
				}
			}
		}
		return v;
	}

	/** Whether the sheet removes {@code part}, everywhere or in the row being drawn. */
	public boolean removed(String part) {
		return sheet.removed(part) || row != null && sheet.instanced() && sheet.removed(part + "@" + row);
	}

	/** {@code argb} (0xAARRGGBB from the sheet) with this frame's alpha and {@code opacity} applied. */
	public int argb(int argb, float opacity) {
		float a = (argb >>> 24) / 255.0F * alpha * opacity;
		return Math.round(Math.max(0.0F, Math.min(1.0F, a)) * 255) << 24 | argb & 0xFFFFFF;
	}

	public boolean over(int x0, int y0, int x1, int y1) {
		return mouseX >= x0 && mouseX < x1 && mouseY >= y0 && mouseY < y1;
	}

	// ---- Transform and clip ----

	public void push() {
		stack[depth * 3] = tx;
		stack[depth * 3 + 1] = ty;
		stack[depth * 3 + 2] = ts;
		depth++;
		canvas.push();
	}

	public void pop() {
		depth--;
		tx = stack[depth * 3];
		ty = stack[depth * 3 + 1];
		ts = stack[depth * 3 + 2];
		canvas.pop();
	}

	public void translate(float x, float y) {
		tx += x * ts;
		ty += y * ts;
		canvas.translate(x, y);
	}

	public void scale(float factor) {
		ts *= factor;
		canvas.scale(factor);
	}

	public void scissor(int x0, int y0, int x1, int y1) {
		clip = new int[] {x0, y0, x1, y1};
		canvas.scissor(x0, y0, x1, y1);
	}

	public void unscissor() {
		clip = null;
		canvas.unscissor();
	}

	/** Remembers a part for the UI lab, moving its local bounds to the screen. */
	public void note(String id, String kind, float x0, float y0, float x1, float y1, Rich text, ItemRef item) {
		note(id, kind, x0, y0, x1, y1, text, item, row, false);
	}

	public void note(String id, String kind, float x0, float y0, float x1, float y1, Rich text, ItemRef item, String ref, boolean removed) {
		if (record) {
			parts.add(new Part(id, kind, x0 * ts + tx, y0 * ts + ty, x1 * ts + tx, y1 * ts + ty, clip, text, item, ref, removed));
		}
	}

	/** Whether the sheet removes {@code part}; if so it is still noted as removed, so the UI lab knows where it was. */
	private boolean gone(String part, String kind, float x0, float y0, float x1, float y1, Rich text, ItemRef item) {
		if (!removed(part)) {
			return false;
		}
		note(part, kind, x0, y0, x1, y1, text, item, row, true);
		return true;
	}

	public void hit(int x0, int y0, int x1, int y1, Runnable action) {
		hits.add(new Hit(x0, y0, x1, y1, action));
	}

	// ---- Primitives ----

	/** A flat rectangle whose colour and opacity the sheet can change. */
	public void fill(String part, int x0, int y0, int x1, int y1, Fill def) {
		int dx = offset(part, "dx");
		int dy = offset(part, "dy");
		x0 += dx;
		x1 += dx;
		y0 += dy;
		y1 += dy;
		if (gone(part, "fill", x0, y0, x1, y1, null, null)) {
			return;
		}
		int color = sheet.color(part + ".color", def.color());
		float opacity = sheet.flt(part + ".opacity", def.opacity());
		canvas.fill(x0, y0, x1, y1, argb(color, opacity));
		note(part, "fill", x0, y0, x1, y1, null, null);
	}

	/** A box: its fill with the corner pixels left out (the pixel squircle) and a hairline around it. */
	public void box(String part, BoxDef def, boolean hover, int x0, int y0, int x1, int y1) {
		int dx = offset(part, "dx");
		int dy = offset(part, "dy");
		x0 += dx;
		x1 += dx;
		y0 += dy;
		y1 += dy;
		if (gone(part, "box", x0, y0, x1, y1, null, null)) {
			return;
		}
		int fill = sheet.color(part + ".fill", def.fill());
		float fillOpacity = sheet.flt(part + ".fill_opacity", def.fillOpacity());
		int border = sheet.color(part + ".border", def.border());
		int hoverFill = sheet.color(part + ".hover_fill", def.hoverFill());
		float hoverFillOpacity = sheet.flt(part + ".hover_fill_opacity", def.hoverFillOpacity());
		int hoverBorder = sheet.color(part + ".hover_border", def.hoverBorder());
		float borderOpacity = sheet.flt(part + ".border_opacity", 1.0F);
		boolean squircle = sheet.bool(part + ".squircle", true);
		int f = argb(hover ? hoverFill : fill, hover ? hoverFillOpacity : fillOpacity);
		int b = argb(hover ? hoverBorder : border, borderOpacity);
		if (!squircle || x1 - x0 < 3 || y1 - y0 < 3) {
			canvas.fill(x0, y0, x1, y1, f);
		} else {
			canvas.fill(x0 + 1, y0, x1 - 1, y0 + 1, f);
			canvas.fill(x0, y0 + 1, x1, y1 - 1, f);
			canvas.fill(x0 + 1, y1 - 1, x1 - 1, y1, f);
		}
		if (border >>> 24 != 0 && borderOpacity > 0 && x1 - x0 >= 2 && y1 - y0 >= 2) {
			canvas.fill(x0 + 1, y0, x1 - 1, y0 + 1, b);
			canvas.fill(x0 + 1, y1 - 1, x1 - 1, y1, b);
			canvas.fill(x0, y0 + 1, x0 + 1, y1 - 1, b);
			canvas.fill(x1 - 1, y0 + 1, x1, y1 - 1, b);
		}
		note(part, "box", x0, y0, x1, y1, null, null);
	}

	public int width(Rich text) {
		int w = 0;
		for (Rich.Span span : text.spans()) {
			w += canvas.width(span.text());
		}
		return w;
	}

	/** The width {@code text} takes at the scale the sheet gives {@code part}. */
	public int width(String part, Rich text, Tx def) {
		return Math.round(width(text) * sheet.flt(part + ".scale", def.scale()));
	}

	/**
	 * One run of text. A span with its own colour (set by game logic, such as a power in its trim's colour)
	 * keeps it; the rest take {@code part}'s colour from the sheet.
	 */
	public void text(String part, Rich text, int x, int y, Tx def) {
		text(part, text, x, y, def, false, -1);
	}

	public void text(String part, Rich text, int x, int y, Tx def, boolean hover, int override) {
		if (text.isEmpty()) {
			return;
		}
		x += offset(part, "dx");
		y += offset(part, "dy");
		float s = sheet.flt(part + ".scale", def.scale());
		int base = sheet.color(part + ".color", def.color());
		int hoverBase = sheet.color(part + ".hover_color", def.hoverColor());
		boolean shadow = sheet.bool(part + ".shadow", def.shadow());
		int color = override >= 0 ? 0xFF000000 | override : hover ? hoverBase : base;
		int w = width(text);
		if (gone(part, "text", x, y, x + w * s, y + 9 * s, text, null)) {
			return;
		}
		if (s != 1.0F) {
			push();
			translate(x, y);
			scale(s);
			run(text, 0, 0, color, shadow);
			pop();
		} else {
			run(text, x, y, color, shadow);
		}
		note(part, "text", x, y, x + w * s, y + 9 * s, text, null);
	}

	private void run(Rich text, int x, int y, int color, boolean shadow) {
		int px = x;
		for (Rich.Span span : text.spans()) {
			int c = span.rgb() >= 0 ? 0xFF000000 | span.rgb() : color;
			canvas.text(span.text(), px, y, argb(c, 1.0F), shadow);
			px += canvas.width(span.text());
		}
	}

	/** {@code text} cut to {@code width} with an ellipsis when it does not fit. */
	public Rich ellipsize(Rich text, int width) {
		if (width(text) <= width) {
			return text;
		}
		String plain = text.plain();
		int room = width - canvas.width("…");
		int end = plain.length();
		while (end > 0 && canvas.width(plain.substring(0, end)) > room) {
			end--;
		}
		return Rich.of(plain.substring(0, end) + "…");
	}

	/** {@code text} broken into lines no wider than {@code width}, on spaces, keeping each run's colour. */
	public List<List<Rich.Span>> wrap(Rich text, int width) {
		List<List<Rich.Span>> lines = new ArrayList<>();
		List<Rich.Span> current = new ArrayList<>();
		int used = 0;
		int space = canvas.width(" ");
		boolean any = false;
		for (Rich.Span span : text.spans()) {
			String s = span.text();
			int i = 0;
			while (i < s.length()) {
				int end = s.indexOf(' ', i);
				boolean spaceAfter = end >= 0;
				if (end < 0) {
					end = s.length();
				}
				String word = s.substring(i, end);
				i = end + (spaceAfter ? 1 : 0);
				if (word.isEmpty()) {
					used += space;
					continue;
				}
				int w = canvas.width(word);
				int gap = any && used > 0 && !current.isEmpty() && !endsWithSpace(current) ? space : 0;
				if (used + gap + w > width && used > 0) {
					lines.add(current);
					current = new ArrayList<>();
					used = 0;
					gap = 0;
				}
				if (gap > 0) {
					current.add(new Rich.Span(" ", span.rgb()));
				}
				current.add(new Rich.Span(word, span.rgb()));
				used += gap + w;
				any = true;
				if (spaceAfter && i >= s.length()) {
					current.add(new Rich.Span(" ", span.rgb()));
					used += space;
				}
			}
		}
		if (!current.isEmpty() || lines.isEmpty()) {
			lines.add(current);
		}
		return lines;
	}

	private static boolean endsWithSpace(List<Rich.Span> line) {
		return line.getLast().text().endsWith(" ");
	}

	/** The number of lines {@code text} takes at {@code width}. */
	public int lineCount(Rich text, int width) {
		return text.isEmpty() ? 0 : wrap(text, width).size();
	}

	/** Wrapped text; returns the height it took. */
	public int lines(String part, Rich text, int x, int y, int width, Tx def) {
		if (text.isEmpty()) {
			return 0;
		}
		int line = line();
		List<List<Rich.Span>> lines = wrap(text, width);
		for (int i = 0; i < lines.size(); i++) {
			Rich one = new Rich(lines.get(i), text.key(), text.args());
			text(part, one, x, y + i * line, def);
		}
		return lines.size() * line;
	}

	/** An item slot as the smithing table shows it, the item inside; an empty item leaves the slot empty. */
	public int slot(String part, ItemRef item, int x, int y) {
		int size = num(part, "size", 18);
		box(part, SLOT, false, x, y, x + size, y + size);
		x += offset(part, "dx");
		y += offset(part, "dy");
		int pad = (size - 16) / 2;
		if (removed(part) || gone(part + ".item", "icon", x + pad, y + pad, x + pad + 16, y + pad + 16, null, item)) {
			return size;
		}
		boolean shown = item != null && !item.empty() && alpha >= 0.5F;
		if (shown) {
			canvas.item(item, x + pad, y + pad);
			if (over(x, y, x + size, y + size)) {
				hovered = item;
			}
		}
		note(part + ".item", "icon", x + pad, y + pad, x + pad + 16, y + pad + 16, null, item);
		return size;
	}

	public static final BoxDef SLOT = new BoxDef("$SUNKEN", 0.85F, "$BORDER_CONTROL");
}
