package dev.vadim.bcenchantments.ui;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The mod's colours (the V/AS look cut down for a block game). {@code client/Theme} re-exports these
 * constants for the HUD; the Field Guide and the smithing panel read them through the {@link Sheet}, so
 * the UI lab can change them.
 */
public final class Palette {
	public static final int GROUND = 0x181818;
	public static final int PANEL = 0x121212;
	public static final int SUNKEN = 0x0E0E0E;
	public static final int SUNKEN_LIGHT = 0x1C1C1C;
	public static final int BORDER = 0x262626;
	public static final int BORDER_CONTROL = 0x3A3A3A;
	public static final int BORDER_HOVER = 0x5A5A5A;

	public static final int TEXT_BRIGHT = 0xF0F0F0;
	public static final int TEXT_BODY = 0xD8D8D8;
	public static final int TEXT_MUTED = 0x9A9A9A;
	public static final int TEXT_LABEL = 0x8A8A8A;
	public static final int TEXT_FAINT = 0x4A4A4A;

	public static final int ACCENT = 0x000000;
	public static final int ON_ACCENT = TEXT_BRIGHT;
	public static final int DANGER = 0xFF6B6B;
	public static final int EMERALD = 0x17DD62;
	public static final int EMERALD_SOFT = 0x0B7A36;
	/** A power at its highest level. */
	public static final int MAX_POWER = 0xFF9F2E;
	public static final int ENOUGH = 0x6FA070;
	public static final int MISSING = 0xB86060;
	public static final int HIGHLIGHT = 0xFFFFFF;

	private static final Map<String, Integer> ALL = new LinkedHashMap<>();

	static {
		ALL.put("GROUND", GROUND);
		ALL.put("PANEL", PANEL);
		ALL.put("SUNKEN", SUNKEN);
		ALL.put("SUNKEN_LIGHT", SUNKEN_LIGHT);
		ALL.put("BORDER", BORDER);
		ALL.put("BORDER_CONTROL", BORDER_CONTROL);
		ALL.put("BORDER_HOVER", BORDER_HOVER);
		ALL.put("TEXT_BRIGHT", TEXT_BRIGHT);
		ALL.put("TEXT_BODY", TEXT_BODY);
		ALL.put("TEXT_MUTED", TEXT_MUTED);
		ALL.put("TEXT_LABEL", TEXT_LABEL);
		ALL.put("TEXT_FAINT", TEXT_FAINT);
		ALL.put("ACCENT", ACCENT);
		ALL.put("ON_ACCENT", ON_ACCENT);
		ALL.put("DANGER", DANGER);
		ALL.put("EMERALD", EMERALD);
		ALL.put("EMERALD_SOFT", EMERALD_SOFT);
		ALL.put("MAX_POWER", MAX_POWER);
		ALL.put("ENOUGH", ENOUGH);
		ALL.put("MISSING", MISSING);
		ALL.put("HIGHLIGHT", HIGHLIGHT);
	}

	private Palette() {
	}

	/** Every palette entry by name, in declaration order. */
	public static Map<String, Integer> defaults() {
		return ALL;
	}
}
