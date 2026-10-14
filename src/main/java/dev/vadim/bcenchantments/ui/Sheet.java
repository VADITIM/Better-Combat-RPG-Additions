package dev.vadim.bcenchantments.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The look of the Field Guide and the smithing panel as one flat table of {@code "part.property"} keys,
 * loaded from {@code assets/bcenchantments/ui/style.json}. Code asks for a key with its default; the sheet
 * remembers every key that was asked for, so the UI lab can list exactly the properties a part has and
 * write a complete file back. Colours are {@code #RRGGBB}, {@code #AARRGGBB} or {@code $NAME} (a palette entry).
 */
public final class Sheet {
	public static final String INT = "int";
	public static final String FLOAT = "float";
	public static final String BOOL = "bool";
	public static final String COLOR = "color";

	/** One property: what it was last asked for with, and its value if the file sets one. */
	public static final class Entry {
		public final String key;
		public final String type;
		public final String def;
		private String raw;
		private Object parsed;

		private Entry(String key, String type, String def, String raw) {
			this.key = key;
			this.type = type;
			this.def = def;
			this.raw = raw;
		}

		/** The value in effect, as text. */
		public String value() {
			return raw != null ? raw : def;
		}

		public boolean isSet() {
			return raw != null;
		}
	}

	private final Map<String, Entry> entries = new HashMap<>();
	/** Values from the file that nothing has asked for yet. */
	private final Map<String, String> pending = new LinkedHashMap<>();
	private int revision;

	public static Sheet parse(String json) {
		Sheet sheet = new Sheet();
		for (Map.Entry<String, Object> e : Json.object(json).entrySet()) {
			sheet.pending.put(e.getKey(), text(e.getValue()));
		}
		return sheet;
	}

	private static String text(Object value) {
		if (value instanceof Double d) {
			return d == Math.rint(d) ? Long.toString(d.longValue()) : Float.toString(d.floatValue());
		}
		return String.valueOf(value);
	}

	/** Bumped on every change, so a cache of laid-out rows knows when it is stale. */
	public int revision() {
		return revision;
	}

	private Entry entry(String key, String type, String def) {
		Entry e = entries.get(key);
		if (e == null) {
			e = new Entry(key, type, def, pending.remove(key));
			entries.put(key, e);
		}
		return e;
	}

	public int num(String key, int def) {
		Entry e = entry(key, INT, Integer.toString(def));
		if (e.parsed == null) {
			e.parsed = parseInt(e.value(), def);
		}
		return (Integer) e.parsed;
	}

	public float flt(String key, float def) {
		Entry e = entry(key, FLOAT, text((double) def));
		if (e.parsed == null) {
			try {
				e.parsed = Float.parseFloat(e.value());
			} catch (NumberFormatException ex) {
				e.parsed = def;
			}
		}
		return (Float) e.parsed;
	}

	public boolean bool(String key, boolean def) {
		Entry e = entry(key, BOOL, Boolean.toString(def));
		if (e.parsed == null) {
			e.parsed = Boolean.parseBoolean(e.value());
		}
		return (Boolean) e.parsed;
	}

	/** A colour as 0xAARRGGBB; {@code def} is {@code $NAME} or hex, and a colour without alpha is opaque. */
	public int color(String key, String def) {
		Entry e = entry(key, COLOR, def);
		if (e.parsed == null) {
			e.parsed = resolve(e.value());
		}
		return (Integer) e.parsed;
	}

	/** A palette entry as 0xRRGGBB, its file value if set. */
	public int palette(String name) {
		Entry e = entry("palette." + name, COLOR, hex(Palette.defaults().getOrDefault(name, 0xFF00FF)));
		if (e.parsed == null) {
			e.parsed = resolve(e.value());
		}
		return (Integer) e.parsed & 0xFFFFFF;
	}

	/** Resolves {@code $NAME}, {@code #RRGGBB} and {@code #AARRGGBB}. */
	public int resolve(String color) {
		String c = color.trim();
		if (c.startsWith("$")) {
			String name = c.substring(1);
			return 0xFF000000 | palette(name);
		}
		if (c.startsWith("#")) {
			String h = c.substring(1);
			try {
				long v = Long.parseLong(h, 16);
				return h.length() > 6 ? (int) v : 0xFF000000 | (int) v;
			} catch (NumberFormatException ex) {
				return 0xFFFF00FF;
			}
		}
		return 0xFFFF00FF;
	}

	private static int parseInt(String s, int def) {
		try {
			return (int) Math.round(Double.parseDouble(s));
		} catch (NumberFormatException ex) {
			return def;
		}
	}

	public static String hex(int rgb) {
		return String.format("#%06X", rgb & 0xFFFFFF);
	}

	// ---- Editing, used by the UI lab ----

	/** Every property asked for so far, sorted by key. */
	public List<Entry> entries() {
		return new ArrayList<>(new TreeMap<>(entries).values());
	}

	public Entry find(String key) {
		return entries.get(key);
	}

	/** The properties of one part: its keys with no further dot, such as {@code stat.label.color}. */
	public List<Entry> of(String part) {
		String prefix = part + ".";
		List<Entry> out = new ArrayList<>();
		for (Entry e : entries()) {
			if (e.key.startsWith(prefix) && e.key.indexOf('.', prefix.length()) < 0) {
				out.add(e);
			}
		}
		return out;
	}

	public void set(String key, String raw) {
		Entry e = entries.get(key);
		if (e == null) {
			pending.put(key, raw);
		} else {
			e.raw = raw;
			e.parsed = null;
		}
		revision++;
		// A palette change moves every colour that refers to it.
		if (key.startsWith("palette.")) {
			for (Entry other : entries.values()) {
				other.parsed = null;
			}
		}
	}

	/** The value the file or an edit set for {@code key}, or null when it has none; never registers the key. */
	public String raw(String key) {
		Entry e = entries.get(key);
		return e != null ? e.raw : pending.get(key);
	}

	/** Every key with a value that starts with {@code prefix}, asked for or not, sorted. */
	public List<String> keys(String prefix) {
		TreeMap<String, String> out = new TreeMap<>();
		for (Entry e : entries.values()) {
			if (e.raw != null && e.key.startsWith(prefix)) {
				out.put(e.key, e.raw);
			}
		}
		for (String key : pending.keySet()) {
			if (key.startsWith(prefix)) {
				out.put(key, key);
			}
		}
		return new ArrayList<>(out.keySet());
	}

	/**
	 * Whether the sheet removes {@code id}: a part ({@code stat.arrow}), one part in one row
	 * ({@code stat.arrow@panel/stat#0}), every row of a kind ({@code row.stat}) or one row by its reference.
	 * {@code hide.} is the old name of {@code remove.} and still read.
	 */
	public boolean removed(String id) {
		return "true".equals(raw("remove." + id)) || "true".equals(raw("hide." + id));
	}

	private int instancedRevision = -1;
	private boolean instanced;

	/** Whether any key is about one part in one row ({@code part@view/anchor...}), so drawing can skip looking for them. */
	public boolean instanced() {
		if (instancedRevision != revision) {
			instanced = false;
			for (Entry e : entries.values()) {
				if (e.raw != null && e.key.indexOf('@') >= 0) {
					instanced = true;
				}
			}
			for (String key : pending.keySet()) {
				if (key.indexOf('@') >= 0) {
					instanced = true;
				}
			}
			instancedRevision = revision;
		}
		return instanced;
	}

	/** Sets {@code key} to {@code raw}, or takes its value away when {@code raw} is null. */
	public void put(String key, String raw) {
		if (raw != null) {
			set(key, raw);
		} else if (entries.containsKey(key)) {
			reset(key);
		} else if (pending.remove(key) != null) {
			revision++;
		}
	}

	/** Replaces every value with {@code other}'s, as if this sheet had just been read from its file. */
	public void load(Sheet other) {
		for (Entry e : entries.values()) {
			e.raw = other.pending.get(e.key);
			e.parsed = null;
		}
		pending.clear();
		for (Map.Entry<String, String> p : other.pending.entrySet()) {
			if (!entries.containsKey(p.getKey())) {
				pending.put(p.getKey(), p.getValue());
			}
		}
		revision++;
	}

	public void reset(String key) {
		Entry e = entries.get(key);
		if (e != null) {
			e.raw = null;
			e.parsed = null;
			revision++;
			if (key.startsWith("palette.")) {
				for (Entry other : entries.values()) {
					other.parsed = null;
				}
			}
		}
	}

	/** The whole sheet: every key asked for with its effective value, palette first, then the rest sorted. */
	public String toJson() {
		Map<String, String> all = new TreeMap<>();
		for (Map.Entry<String, Integer> p : Palette.defaults().entrySet()) {
			Entry e = entries.get("palette." + p.getKey());
			all.put("palette." + p.getKey(), e != null ? e.value() : pending.getOrDefault("palette." + p.getKey(), hex(p.getValue())));
		}
		for (Entry e : entries.values()) {
			all.put(e.key, e.value());
		}
		all.putAll(pending);
		StringBuilder out = new StringBuilder("{\n");
		boolean first = true;
		for (Map.Entry<String, String> e : all.entrySet()) {
			if (!first) {
				out.append(",\n");
			}
			first = false;
			out.append('\t').append(Json.quote(e.getKey())).append(": ");
			Entry known = entries.get(e.getKey());
			String type = known != null ? known.type : null;
			if (INT.equals(type) || FLOAT.equals(type) || BOOL.equals(type) || type == null && e.getKey().indexOf(':') < 0 && e.getValue().matches("-?\\d+(\\.\\d+)?|true|false")) {
				out.append(e.getValue());
			} else {
				out.append(Json.quote(e.getValue()));
			}
		}
		return out.append("\n}\n").toString();
	}
}
