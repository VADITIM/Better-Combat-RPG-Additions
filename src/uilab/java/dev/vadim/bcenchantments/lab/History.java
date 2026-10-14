package dev.vadim.bcenchantments.lab;

import dev.vadim.bcenchantments.ui.Sheet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Every edit the lab makes, to the sheet or to the lang file, so Ctrl+Z and Ctrl+Y can step through them.
 * Edits made inside {@link #group} undo as one; typing into the same field within a moment counts as one edit.
 */
final class History {
	private static final long TYPING_MS = 1000;
	private static final int LIMIT = 500;

	/** One key going from {@code before} to {@code after}; null means unset in the sheet. */
	private record Change(boolean lang, String key, String before, String after) {
	}

	private final Sheet sheet;
	private final Lang lang;
	private final Deque<List<Change>> undo = new ArrayDeque<>();
	private final Deque<List<Change>> redo = new ArrayDeque<>();
	private List<Change> open;
	private String typingKey;
	private long typingAt;

	History(Sheet sheet, Lang lang) {
		this.sheet = sheet;
		this.lang = lang;
	}

	/** Sets a sheet key; {@code value} null takes it back to its default. */
	void set(String key, String value) {
		set(key, value, false);
	}

	/** As {@link #set(String, String)}; {@code typing} merges it with the last edit if that was typing into the same key. */
	void set(String key, String value, boolean typing) {
		String before = sheet.raw(key);
		if (same(before, value)) {
			return;
		}
		sheet.put(key, value);
		record(new Change(false, key, before, value), typing);
	}

	void lang(String key, String value) {
		String before = lang.raw(key);
		if (same(before, value)) {
			return;
		}
		lang.set(key, value);
		record(new Change(true, key, before, value), true);
	}

	/** Runs {@code edits} so that they undo as one step. */
	void group(Runnable edits) {
		if (open != null) {
			edits.run();
			return;
		}
		open = new ArrayList<>();
		try {
			edits.run();
		} finally {
			List<Change> done = open;
			open = null;
			if (!done.isEmpty()) {
				push(done);
			}
			typingKey = null;
		}
	}

	private void record(Change change, boolean typing) {
		if (open != null) {
			open.add(change);
			return;
		}
		long now = System.currentTimeMillis();
		String tag = (change.lang ? "lang:" : "sheet:") + change.key;
		if (typing && tag.equals(typingKey) && now - typingAt < TYPING_MS && !undo.isEmpty()) {
			undo.peek().add(change);
			redo.clear();
		} else {
			List<Change> step = new ArrayList<>();
			step.add(change);
			push(step);
		}
		typingKey = typing ? tag : null;
		typingAt = now;
	}

	private void push(List<Change> step) {
		undo.push(step);
		if (undo.size() > LIMIT) {
			undo.removeLast();
		}
		redo.clear();
	}

	boolean undo() {
		List<Change> step = undo.poll();
		if (step == null) {
			return false;
		}
		for (int i = step.size() - 1; i >= 0; i--) {
			apply(step.get(i), true);
		}
		redo.push(step);
		typingKey = null;
		return true;
	}

	boolean redo() {
		List<Change> step = redo.poll();
		if (step == null) {
			return false;
		}
		for (Change change : step) {
			apply(change, false);
		}
		undo.push(step);
		typingKey = null;
		return true;
	}

	private void apply(Change change, boolean back) {
		String value = back ? change.before : change.after;
		if (change.lang) {
			lang.set(change.key, value);
		} else {
			sheet.put(change.key, value);
		}
	}

	void clear() {
		undo.clear();
		redo.clear();
		typingKey = null;
	}

	int undoable() {
		return undo.size();
	}

	int redoable() {
		return redo.size();
	}

	private static boolean same(String a, String b) {
		return a == null ? b == null : a.equals(b);
	}
}
