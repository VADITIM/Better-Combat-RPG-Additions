package dev.vadim.bcenchantments.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Which rows a view shows and in what order, as the sheet changes it: rows it removes, moves and adds. All
 * live in {@code style.json} next to the look, so the UI lab edits them and the next build ships them.
 * <ul>
 * <li>{@code "remove.<id>": true} removes a part ({@code stat.arrow}), one part in one row
 * ({@code stat.arrow@panel/stat#0}), every row of a kind ({@code row.stat}) or one row by its reference
 * ({@code panel/stat[bcenchantments.panel.stat.speed]}). The old name {@code hide.} still works.</li>
 * <li>{@code "move.<ref>": "after:<anchor>"} (or {@code before:}) puts a row next to another; if that one is
 * not shown, the row stays where the code put it.</li>
 * <li>{@code "part@ref.dx"} and {@code .dy} move one part in one row on top of the part's own offset.</li>
 * <li>{@code "add.<n>:<field>"} is an added row: {@code view} ({@code panel}, {@code guide.0} to {@code guide.3}),
 * {@code type} (see {@link #TYPES}), {@code at} (an anchor) and {@code place} ({@code after} or {@code before}),
 * then its content: {@code text}, {@code sub}, {@code items} (item ids, comma separated), {@code tone},
 * {@code align}, {@code size} and {@code overlay} (takes no height, so it sits over the row above).</li>
 * </ul>
 * An anchor names a row in its view: {@code kind[lang key]} when its {@link Row#label()} has a key,
 * {@code kind#i} for the i-th row of its kind without one (a repeat of a keyed row gets {@code #i} too),
 * {@code add.<n>} for an added row, {@code ^} for the top and {@code $} for the bottom. Parts of an added row
 * are named {@code add.<n>.text}, {@code add.<n>.slot} and so on, so each one has its own look.
 */
public final class Layout {
	public static final List<String> TYPES = List.of("label", "heading", "title", "stat", "icon", "icons", "entry", "rule", "space");
	public static final List<String> TONES = List.of("bright", "body", "muted", "label", "faint", "danger", "enough", "missing", "max");
	public static final List<String> ALIGNS = List.of("left", "center", "right");

	private static Sheet cachedSheet;
	private static int cachedRevision = -1;
	private static Map<String, List<Added>> cachedAdds = Map.of();

	private Layout() {
	}

	/** A row placed in a view, with the reference ({@code view/anchor}) the UI lab hides it or adds next to it by. */
	public record Placed(Row row, String ref, String anchor) implements Row {
		@Override
		public String kind() {
			return row.kind();
		}

		@Override
		public int height(Ctx c, int width) {
			return row.height(c, width);
		}

		@Override
		public void draw(Ctx c, int x, int y, int width) {
			row.draw(c, x, y, width);
		}

		@Override
		public Rich label() {
			return row.label();
		}
	}

	/** The field {@code field} of added row {@code id} ({@code add.<n>}), or {@code def}. */
	public static String field(Sheet s, String id, String field, String def) {
		String v = s.raw(id + ":" + field);
		return v != null ? v : def;
	}

	/** The ids of every added row ({@code add.<n>}), in order of {@code n}. */
	public static List<String> addedIds(Sheet s) {
		List<Integer> ns = new ArrayList<>();
		for (String key : s.keys("add.")) {
			int colon = key.indexOf(':');
			if (colon > 4) {
				try {
					int n = Integer.parseInt(key.substring(4, colon));
					if (!ns.contains(n)) {
						ns.add(n);
					}
				} catch (NumberFormatException ignored) {
					// Not an added row's field.
				}
			}
		}
		ns.sort(null);
		List<String> ids = new ArrayList<>();
		for (int n : ns) {
			ids.add("add." + n);
		}
		return ids;
	}

	/** The next free added-row id. */
	public static String nextId(Sheet s) {
		int max = 0;
		for (String id : addedIds(s)) {
			max = Math.max(max, Integer.parseInt(id.substring(4)));
		}
		return "add." + (max + 1);
	}

	/** A row placed by an anchor rather than by the code: an added row, or a row the sheet moves. {@code fallback} is where a moved row goes if its anchor is missing. */
	private record Floating(Placed placed, String at, boolean before, String fallback) {
	}

	/**
	 * {@code rows} for view {@code view} as the sheet arranges them, each as a {@link Placed}: its added rows put
	 * in, its moved rows ({@code "move.<ref>": "after:<anchor>"} or {@code "before:<anchor>"}) taken to their
	 * anchor, and its removed rows taken out.
	 */
	public static List<Row> apply(Sheet s, String view, List<Row> rows) {
		List<Placed> out = new ArrayList<>(rows.size() + 4);
		List<Floating> left = new ArrayList<>();
		Map<String, Integer> counts = new HashMap<>();
		String previous = "^";
		for (Row row : rows) {
			String anchor = anchor(row, counts);
			Placed placed = new Placed(row, view + "/" + anchor, anchor);
			String move = s.raw("move." + placed.ref());
			int colon = move == null ? -1 : move.indexOf(':');
			if (colon > 0) {
				left.add(new Floating(placed, move.substring(colon + 1), move.substring(0, colon).equals("before"), previous));
			} else {
				out.add(placed);
			}
			previous = anchor;
		}
		for (Added add : adds(s).getOrDefault(view, List.of())) {
			left.add(new Floating(new Placed(add, view + "/" + add.id, add.id), add.at, add.before, null));
		}
		Map<Placed, Floating> floating = new IdentityHashMap<>();
		settle(out, left, floating);
		if (!left.isEmpty()) {
			// Moved rows whose anchor is not shown go back after the row they followed in the code.
			List<Floating> back = new ArrayList<>();
			for (Floating f : left) {
				back.add(f.fallback != null ? new Floating(f.placed, f.fallback, false, null) : f);
			}
			left = back;
			settle(out, left, floating);
		}
		List<Row> shown = new ArrayList<>(out.size());
		for (Placed p : out) {
			if (!s.removed("row." + p.kind()) && !s.removed(p.ref())) {
				shown.add(p);
			}
		}
		return shown;
	}

	/** Puts every floating row whose anchor is in {@code out} there, over and over while that places more. */
	private static void settle(List<Placed> out, List<Floating> left, Map<Placed, Floating> floating) {
		boolean progress = true;
		while (!left.isEmpty() && progress) {
			progress = false;
			for (int a = 0; a < left.size(); a++) {
				Floating f = left.get(a);
				int index = place(out, f, floating);
				if (index >= 0) {
					out.add(index, f.placed);
					floating.put(f.placed, f);
					left.remove(a--);
					progress = true;
				}
			}
		}
	}

	private static String anchor(Row row, Map<String, Integer> counts) {
		Rich label = row.label();
		String base = label != null && label.key() != null ? row.kind() + "[" + label.key() + "]" : row.kind();
		int n = counts.merge(base, 1, Integer::sum) - 1;
		return base.equals(row.kind()) || n > 0 ? base + "#" + n : base;
	}

	/** Where {@code f} goes in {@code out}, or -1 if its anchor is not there (yet). Rows placed at one anchor keep their order. */
	private static int place(List<Placed> out, Floating f, Map<Placed, Floating> floating) {
		boolean before = f.before;
		int index = -1;
		if (f.at.equals("^")) {
			index = 0;
		} else if (f.at.equals("$")) {
			return out.size();
		} else {
			for (int i = 0; i < out.size(); i++) {
				if (out.get(i).anchor().equals(f.at)) {
					index = before ? i : i + 1;
					break;
				}
			}
			if (index < 0) {
				return -1;
			}
		}
		while (index < out.size() && floating.get(out.get(index)) instanceof Floating other && other.at.equals(f.at) && other.before == before) {
			index++;
		}
		return index;
	}

	private static Map<String, List<Added>> adds(Sheet s) {
		if (s != cachedSheet || s.revision() != cachedRevision) {
			Map<String, List<Added>> byView = new HashMap<>();
			for (String id : addedIds(s)) {
				Added add = new Added(s, id);
				byView.computeIfAbsent(add.view, k -> new ArrayList<>()).add(add);
			}
			cachedAdds = byView;
			cachedSheet = s;
			cachedRevision = s.revision();
		}
		return cachedAdds;
	}

	private static int parse(String s, int def) {
		try {
			return Integer.parseInt(s.trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}

	/** A row the sheet adds. */
	public static final class Added implements Row {
		private static final Ctx.Fill RULE = new Ctx.Fill("$BORDER", 1.0F);

		final String id;
		final String view;
		final String type;
		final String at;
		final boolean before;
		final Rich text;
		final Rich sub;
		final List<ItemRef> items = new ArrayList<>();
		final String tone;
		final String align;
		final int size;
		final boolean overlay;

		Added(Sheet s, String id) {
			this.id = id;
			view = field(s, id, "view", "panel");
			type = field(s, id, "type", "label");
			at = field(s, id, "at", "$");
			before = field(s, id, "place", "after").equals("before");
			text = Rich.of(field(s, id, "text", ""));
			String subText = field(s, id, "sub", "");
			sub = subText.isEmpty() ? null : Rich.of(subText);
			for (String item : field(s, id, "items", "").split(",")) {
				if (!item.isBlank()) {
					items.add(ItemRef.of(item.trim()));
				}
			}
			tone = field(s, id, "tone", type.equals("heading") ? "muted" : "body");
			align = field(s, id, "align", "left");
			size = parse(field(s, id, "size", ""), type.equals("rule") ? 5 : 6);
			overlay = field(s, id, "overlay", "false").equals("true");
		}

		@Override
		public String kind() {
			return id;
		}

		private ItemRef item(int i) {
			return i < items.size() ? items.get(i) : ItemRef.EMPTY;
		}

		private int slot(Ctx c) {
			return c.num(id + ".slot", "size", 18);
		}

		private int gap(Ctx c) {
			return c.num(id, "gap", type.equals("icons") || type.equals("entry") ? 2 : 4);
		}

		/** Where the text column starts in an entry. */
		private int textX(Ctx c) {
			return Math.max(1, items.size()) * (slot(c) + gap(c)) + c.num(id, "text_gap", 4) - gap(c);
		}

		@Override
		public int height(Ctx c, int width) {
			return overlay ? 0 : naturalHeight(c, width);
		}

		private int naturalHeight(Ctx c, int width) {
			int line = c.line();
			return switch (type) {
				case "heading" -> c.num("ui", "section_gap", 6) + line + 2;
				case "title" -> c.num("ui", "section_gap", 6) + Math.round(c.sheet.flt(id + ".text.scale", 2.0F) * line) + 2;
				case "stat" -> line;
				case "icon" -> Math.max(slot(c), line + (sub == null ? 0 : c.lineCount(sub, width - slot(c) - gap(c)) * line)) + 1;
				case "icons" -> slot(c);
				case "entry" -> Math.max(slot(c), line + (sub == null ? 0 : c.lineCount(sub, width - textX(c)) * line));
				case "rule", "space" -> size;
				default -> align.equals("left") ? Math.max(1, c.lineCount(text, width)) * line : line + 2;
			};
		}

		private int alignX(int x, int width, int w) {
			return switch (align) {
				case "center" -> x + (width - w) / 2;
				case "right" -> x + width - w;
				default -> x;
			};
		}

		@Override
		public void draw(Ctx c, int x, int y, int width) {
			Ctx.Tx style = Rows.tx(tone);
			switch (type) {
				case "heading" -> {
					int gap = c.num("ui", "section_gap", 6);
					c.fill(id + ".rule", x, y + gap / 2 - 1, x + width, y + gap / 2, RULE);
					c.text(id + ".text", text, alignX(x, width, c.width(id + ".text", text, style)), y + gap, style);
				}
				case "title" -> {
					Ctx.Tx big = style.scaled(2.0F);
					int gap = c.num("ui", "section_gap", 6);
					c.fill(id + ".rule", x, y + gap / 2 - 1, x + width, y + gap / 2, RULE);
					int w = c.width(id + ".text", text, big);
					c.text(id + ".text", text, align.equals("left") ? x : alignX(x, width, w), y + gap + 1, big);
				}
				case "stat" -> {
					c.text(id + ".text", text, x, y, Rows.tx("label"));
					Rich value = sub != null ? sub : Rich.EMPTY;
					c.text(id + ".value", value, x + width - c.width(id + ".value", value, style), y, style);
				}
				case "icon" -> {
					int size = c.slot(id + ".slot", item(0), x, y);
					int tx = x + size + gap(c);
					c.text(id + ".text", c.ellipsize(text, width - size - gap(c)), tx, y + (sub == null ? 5 : 1), style);
					if (sub != null) {
						c.lines(id + ".sub", sub, tx, y + 1 + c.line(), width - size - gap(c), Rows.tx("muted"));
					}
				}
				case "icons" -> {
					int step = slot(c) + gap(c);
					int w = items.size() * step - gap(c);
					int sx = alignX(x, width, w);
					for (int i = 0; i < items.size(); i++) {
						c.slot(id + ".slot", items.get(i), sx + i * step, y);
					}
				}
				case "entry" -> {
					int step = slot(c) + gap(c);
					for (int i = 0; i < Math.max(1, items.size()); i++) {
						c.slot(id + ".slot", item(i), x + i * step, y);
					}
					int tx = x + textX(c);
					c.text(id + ".text", text, tx, y + 1, Ctx.Tx.shadowed(Rows.tone(tone.equals("body") ? "bright" : tone)));
					if (sub != null) {
						c.lines(id + ".sub", sub, tx, y + 1 + c.line(), width - textX(c), Rows.tx("body"));
					}
				}
				case "rule" -> c.fill(id + ".rule", x, y + size / 2, x + width, y + size / 2 + 1, RULE);
				case "space" -> {
				}
				default -> {
					if (align.equals("left")) {
						c.lines(id + ".text", text, x, y, width, style);
					} else {
						c.text(id + ".text", text, alignX(x, width, c.width(id + ".text", text, style)), y + 1, style);
					}
				}
			}
			if (overlay) {
				c.note("row." + id, "row", x, y, x + width, y + naturalHeight(c, width), null, null, c.view + "/" + id, false);
			}
		}
	}
}
