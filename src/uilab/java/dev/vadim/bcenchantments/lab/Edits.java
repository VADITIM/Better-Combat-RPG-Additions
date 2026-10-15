package dev.vadim.bcenchantments.lab;

import dev.vadim.bcenchantments.ui.Ctx;
import dev.vadim.bcenchantments.ui.Layout;
import dev.vadim.bcenchantments.ui.Row;
import dev.vadim.bcenchantments.ui.Sheet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Every change the lab makes to what is on screen: moving a part, moving rows, removing and restoring,
 * adding, duplicating and deleting rows, and rewording. Each one goes through {@link History} as one step, so
 * Ctrl+Z takes it back whole.
 */
final class Edits {
	private final Stage stage;
	private final Sheet sheet;
	final History history;

	Edits(Stage stage, History history) {
		this.stage = stage;
		this.sheet = stage.sheet;
		this.history = history;
	}

	// ---- Parts ----

	/** The id a move or a removal of {@code p} changes: an item's slot rather than the item alone. */
	static String target(Ctx.Part p) {
		return p.id().endsWith(".item") ? p.id().substring(0, p.id().length() - ".item".length()) : p.id();
	}

	/** Whether dragging {@code p} moves it: not a row, the window itself, the scroll bar or the cost tip. */
	static boolean movable(Ctx.Part p) {
		String id = p.id();
		return !p.kind().equals("row") && !window(p) && !id.startsWith("scroll.") && !id.startsWith("tip.");
	}

	/** Whether {@code p} is a view's window itself, which can be recoloured but not moved or removed. */
	static boolean window(Ctx.Part p) {
		return switch (p.id()) {
			case "panel.box", "guide.box", "difficulty.box" -> true;
			default -> false;
		};
	}

	/** The key for {@code p}'s offset along {@code axis}: its own, or with {@code only} the one for it in its row. */
	static String offsetKey(Ctx.Part p, boolean only, String axis) {
		String id = target(p);
		return only && p.ref() != null ? id + "@" + p.ref() + "." + axis : id + "." + axis;
	}

	int offset(String key) {
		String raw = sheet.raw(key);
		if (raw == null) {
			return 0;
		}
		try {
			return (int) Math.round(Double.parseDouble(raw));
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	/** Moves {@code p} by ({@code dx}, {@code dy}); {@code typing} merges it with the last nudge of the same part. */
	void nudge(Ctx.Part p, boolean only, int dx, int dy, boolean typing) {
		history.group(() -> {
			if (dx != 0) {
				String key = offsetKey(p, only, "dx");
				setOffset(key, offset(key) + dx, typing);
			}
			if (dy != 0) {
				String key = offsetKey(p, only, "dy");
				setOffset(key, offset(key) + dy, typing);
			}
		});
	}

	/** Sets an offset; one in a row goes away at 0, the part's own stays as a number. */
	void setOffset(String key, int value, boolean typing) {
		history.set(key, value == 0 && key.indexOf('@') >= 0 ? null : Integer.toString(value), typing);
	}

	/** Puts {@code p} back where the code draws it: its own offset in this row and, unless {@code only}, everywhere. */
	void resetPosition(Ctx.Part p, boolean only) {
		history.group(() -> {
			for (String axis : List.of("dx", "dy")) {
				if (p.ref() != null) {
					history.set(offsetKey(p, true, axis), null);
				}
				if (!only) {
					history.set(offsetKey(p, false, axis), "0");
				}
			}
		});
	}

	/** Every look property of {@code p} (not its position) back to its default. */
	void resetLook(Ctx.Part p) {
		history.group(() -> {
			for (String id : styleIds(p)) {
				for (Sheet.Entry e : sheet.of(id)) {
					if (!e.key.endsWith(".dx") && !e.key.endsWith(".dy")) {
						history.set(e.key, null);
					}
				}
			}
		});
	}

	/** The ids whose properties style {@code p}: the part, and for an item its slot. */
	static List<String> styleIds(Ctx.Part p) {
		return p.id().endsWith(".item") ? List.of(target(p), p.id()) : List.of(p.id());
	}

	// ---- Removing ----

	/** The added row ({@code add.<n>}) {@code p} is or belongs to, or null. */
	static String addedOf(Ctx.Part p) {
		String ref = p.ref();
		if (ref != null) {
			String anchor = ref.substring(ref.indexOf('/') + 1);
			if (anchor.startsWith("add.")) {
				return anchor;
			}
		}
		return null;
	}

	/**
	 * Removes {@code p}: a row (an added one is deleted outright), or a part, everywhere or with {@code only} in its
	 * row. A part of an added label, heading, title or rule takes its row with it, since the row is nothing else.
	 * Returns what it did, for the status line.
	 */
	String remove(Ctx.Part p, boolean only) {
		String added = addedOf(p);
		if (p.kind().equals("row")) {
			if (added != null) {
				deleteAdded(added);
				return "Deleted " + added + ".";
			}
			history.set("remove." + p.ref(), "true");
			return "Removed the row " + anchorOf(p.ref()) + ".";
		}
		if (added != null && List.of("label", "heading", "title", "rule", "space").contains(Layout.field(sheet, added, "type", "label"))) {
			deleteAdded(added);
			return "Deleted " + added + ".";
		}
		String id = target(p);
		if (only && p.ref() != null) {
			history.set("remove." + id + "@" + p.ref(), "true");
			return "Removed " + id + " from " + anchorOf(p.ref()) + ".";
		}
		history.set("remove." + id, "true");
		return "Removed every " + id + ".";
	}

	/** Removes every row of {@code p}'s kind. */
	void removeKind(Ctx.Part row) {
		history.set("remove." + row.id(), "true");
	}

	/** Every removal in the sheet, as its key. */
	List<String> removals() {
		List<String> out = new ArrayList<>(sheet.keys("remove."));
		out.addAll(sheet.keys("hide."));
		return out;
	}

	/** A removal key as a person would say it. */
	static String describe(String key) {
		String id = key.substring(key.indexOf('.') + 1);
		int at = id.indexOf('@');
		if (at >= 0) {
			return id.substring(0, at) + " in " + anchorOf(id.substring(at + 1));
		}
		if (id.startsWith("row.")) {
			return "every " + id.substring(4) + " row";
		}
		if (id.contains("/")) {
			return "row " + anchorOf(id) + " (" + id.substring(0, id.indexOf('/')) + ")";
		}
		return id + " (everywhere)";
	}

	void restore(String key) {
		history.set(key, null);
	}

	static String anchorOf(String ref) {
		return ref.substring(ref.indexOf('/') + 1);
	}

	static String viewOf(String ref) {
		return ref.substring(0, ref.indexOf('/'));
	}

	// ---- Rows ----

	/** The rows of the view on show, in the order shown. */
	List<Layout.Placed> order() {
		List<Layout.Placed> out = new ArrayList<>();
		for (Row row : stage.ctx.rows) {
			if (row instanceof Layout.Placed p) {
				out.add(p);
			}
		}
		return out;
	}

	Layout.Placed placed(String ref) {
		for (Layout.Placed p : order()) {
			if (p.ref().equals(ref)) {
				return p;
			}
		}
		return null;
	}

	private static boolean opensSection(Layout.Placed p, Sheet sheet) {
		String kind = p.kind();
		if (kind.startsWith("add.")) {
			kind = Layout.field(sheet, kind, "type", "label");
		}
		return kind.equals("heading") || kind.equals("title");
	}

	/** {@code p}, and when it is a heading or title every row under it up to the next one: what a drag of it moves. */
	List<Layout.Placed> group(Layout.Placed p) {
		List<Layout.Placed> order = order();
		int i = order.indexOf(p);
		List<Layout.Placed> out = new ArrayList<>();
		out.add(p);
		if (i >= 0 && opensSection(p, sheet)) {
			for (int j = i + 1; j < order.size() && !opensSection(order.get(j), sheet); j++) {
				out.add(order.get(j));
			}
		}
		return out;
	}

	/** Where a floating row sits as {place, anchor}, or null for a row where the code put it. */
	private String[] placement(String view, String anchor) {
		if (anchor.startsWith("add.")) {
			return new String[] {Layout.field(sheet, anchor, "place", "after"), Layout.field(sheet, anchor, "at", "$")};
		}
		String move = sheet.raw("move." + view + "/" + anchor);
		int colon = move == null ? -1 : move.indexOf(':');
		return colon > 0 ? new String[] {move.substring(0, colon), move.substring(colon + 1)} : null;
	}

	private void setPlacement(String view, String anchor, String place, String at) {
		if (anchor.startsWith("add.")) {
			history.set(anchor + ":at", at);
			history.set(anchor + ":place", place);
		} else {
			history.set("move." + view + "/" + anchor, place + ":" + at);
		}
	}

	/** Every row of {@code view} that sits by an anchor, shown or not, as its anchor. */
	private List<String> floating(String view) {
		List<String> out = new ArrayList<>();
		for (String id : Layout.addedIds(sheet)) {
			if (Layout.field(sheet, id, "view", "panel").equals(view)) {
				out.add(id);
			}
		}
		String prefix = "move." + view + "/";
		for (String key : sheet.keys(prefix)) {
			out.add(key.substring(prefix.length()));
		}
		return out;
	}

	/**
	 * Moves {@code rows} (in order, all of the view on show) to just after {@code after} ({@code ^} for the top).
	 * Rows that hung on the moved ones stay where they are, and rows that hung on {@code after} now follow the
	 * last moved row, so nothing else on screen shifts.
	 */
	boolean moveRows(List<Layout.Placed> rows, String after) {
		if (rows.isEmpty()) {
			return false;
		}
		String view = viewOf(rows.getFirst().ref());
		Set<String> moving = new HashSet<>();
		for (Layout.Placed p : rows) {
			moving.add(p.anchor());
		}
		if (moving.contains(after)) {
			return false;
		}
		List<Layout.Placed> order = order();
		String last = rows.getLast().anchor();
		history.group(() -> {
			for (String anchor : floating(view)) {
				if (moving.contains(anchor)) {
					continue;
				}
				String[] at = placement(view, anchor);
				if (at == null) {
					continue;
				}
				if (moving.contains(at[1])) {
					setPlacement(view, anchor, "after", before(order, anchor, moving, rows.getFirst().anchor()));
				} else if (at[1].equals(after) && at[0].equals("after")) {
					setPlacement(view, anchor, "after", last);
				}
			}
			String prev = after;
			for (Layout.Placed p : rows) {
				setPlacement(view, p.anchor(), "after", prev);
				prev = p.anchor();
			}
		});
		return true;
	}

	/** The nearest row above {@code anchor} in {@code order} that is not moving ({@code ^} if none); {@code fallback} stands in for a row not shown. */
	private static String before(List<Layout.Placed> order, String anchor, Set<String> moving, String fallback) {
		int i = indexOf(order, anchor);
		if (i < 0) {
			i = indexOf(order, fallback);
		}
		for (int j = i - 1; j >= 0; j--) {
			if (!moving.contains(order.get(j).anchor())) {
				return order.get(j).anchor();
			}
		}
		return "^";
	}

	private static int indexOf(List<Layout.Placed> order, String anchor) {
		for (int i = 0; i < order.size(); i++) {
			if (order.get(i).anchor().equals(anchor)) {
				return i;
			}
		}
		return -1;
	}

	/** Moves the row {@code p} one place up ({@code by} -1) or down (1). */
	boolean step(Layout.Placed p, int by) {
		List<Layout.Placed> order = order();
		int i = order.indexOf(p);
		if (i < 0) {
			return false;
		}
		if (by < 0) {
			return i > 0 && moveRows(List.of(p), i >= 2 ? order.get(i - 2).anchor() : "^");
		}
		return i < order.size() - 1 && moveRows(List.of(p), order.get(i + 1).anchor());
	}

	/** Puts every moved row of the view on show back where the code puts it. */
	void unmoveAll(String view) {
		history.group(() -> {
			for (String key : sheet.keys("move." + view + "/")) {
				history.set(key, null);
			}
		});
	}

	// ---- Added rows ----

	/** Adds a row of {@code type} to {@code view} just after {@code after}; returns its id. */
	String addRow(String view, String after, String type) {
		String id = Layout.nextId(sheet);
		history.group(() -> {
			for (String anchor : floating(view)) {
				String[] at = placement(view, anchor);
				if (at != null && at[1].equals(after) && at[0].equals("after")) {
					setPlacement(view, anchor, "after", id);
				}
			}
			history.set(id + ":view", view);
			history.set(id + ":type", type);
			history.set(id + ":at", after);
			history.set(id + ":place", "after");
			switch (type) {
				case "label" -> history.set(id + ":text", "New label");
				case "heading" -> history.set(id + ":text", "New heading");
				case "title" -> history.set(id + ":text", "New title");
				case "stat" -> {
					history.set(id + ":text", "Stat");
					history.set(id + ":sub", "0");
				}
				case "icon" -> {
					history.set(id + ":items", "emerald");
					history.set(id + ":text", "New icon");
					history.set(id + ":sub", "Text under it");
				}
				case "icons" -> history.set(id + ":items", "emerald, diamond");
				case "entry" -> {
					history.set(id + ":items", "emerald");
					history.set(id + ":text", "New entry");
					history.set(id + ":sub", "What it does.");
				}
				default -> {
				}
			}
		});
		return id;
	}

	/** A copy of added row {@code id}, look and all, just after it; returns the copy's id. */
	String duplicate(String id) {
		String copy = Layout.nextId(sheet);
		String view = Layout.field(sheet, id, "view", "panel");
		history.group(() -> {
			for (String anchor : floating(view)) {
				String[] at = placement(view, anchor);
				if (at != null && at[1].equals(id) && at[0].equals("after")) {
					setPlacement(view, anchor, "after", copy);
				}
			}
			for (String key : sheet.keys(id + ":")) {
				history.set(copy + key.substring(id.length()), sheet.raw(key));
			}
			for (String key : sheet.keys(id + ".")) {
				history.set(copy + key.substring(id.length()), sheet.raw(key));
			}
			history.set(copy + ":at", id);
			history.set(copy + ":place", "after");
		});
		return copy;
	}

	/** Deletes added row {@code id} with its look; rows that hung on it take its place. */
	void deleteAdded(String id) {
		String view = Layout.field(sheet, id, "view", "panel");
		String at = Layout.field(sheet, id, "at", "$");
		String place = Layout.field(sheet, id, "place", "after");
		history.group(() -> {
			for (String anchor : floating(view)) {
				String[] p = placement(view, anchor);
				if (p != null && p[1].equals(id) && !anchor.equals(id)) {
					setPlacement(view, anchor, place, at);
				}
			}
			for (String key : sheet.keys(id + ":")) {
				history.set(key, null);
			}
			for (String key : sheet.keys(id + ".")) {
				history.set(key, null);
			}
			for (String key : sheet.keys("row." + id + ".")) {
				history.set(key, null);
			}
			for (String key : new ArrayList<>(sheet.keys(""))) {
				if (key.contains("@" + view + "/" + id + ".") || key.endsWith("@" + view + "/" + id) || key.equals("remove." + view + "/" + id)
					|| key.startsWith("remove." + id + ".") || key.equals("remove.row." + id)) {
					history.set(key, null);
				}
			}
		});
	}

	/** Sets field {@code field} of added row {@code id}; {@code null} takes it back to its default. */
	void field(String id, String field, String value) {
		history.set(id + ":" + field, value);
	}

	// ---- Wording ----

	/** Where the words of a text part live: a lang key, or a field of an added row. */
	record Words(boolean lang, String key, String value) {
	}

	/** What a double-click on {@code p} edits, or null if the game sets it. */
	Words words(Ctx.Part p) {
		String added = addedOf(p);
		if (added != null) {
			String rest = p.id().startsWith(added + ".") ? p.id().substring(added.length() + 1) : "";
			String field = switch (rest) {
				case "text" -> "text";
				case "sub", "value" -> "sub";
				case "slot", "slot.item" -> "items";
				default -> null;
			};
			return field == null ? null : new Words(false, added + ":" + field, Layout.field(sheet, added, field, ""));
		}
		if (p.text() != null && p.text().key() != null && stage.lang.editable(p.text().key())) {
			return new Words(true, p.text().key(), stage.lang.raw(p.text().key()));
		}
		return null;
	}

	/** Shows {@code value} at once, outside the history, while it is being typed. */
	void preview(Words w, String value) {
		if (w.lang) {
			stage.lang.set(w.key, value);
		} else {
			sheet.put(w.key, value);
		}
	}

	/** Ends a typing preview: back to where it started, then {@code value} as one step (null to cancel). */
	void commit(Words w, String value) {
		preview(w, w.value);
		if (value == null || value.equals(w.value)) {
			return;
		}
		if (w.lang) {
			history.lang(w.key, value);
		} else {
			history.set(w.key, value);
		}
	}

	static String title(String type) {
		return type.substring(0, 1).toUpperCase(Locale.ROOT) + type.substring(1);
	}
}
