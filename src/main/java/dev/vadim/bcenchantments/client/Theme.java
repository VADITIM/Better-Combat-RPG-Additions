package dev.vadim.bcenchantments.client;

import dev.vadim.bcenchantments.progression.Refinement;
import dev.vadim.bcenchantments.ui.Palette;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

/**
 * The mod's look in one place, taken from V/AS (VAD/OS APPLICATION SYSTEMS) and cut down for a block
 * game: a near-black ground, hairline borders instead of shadows, flat slabs, and a monochrome accent,
 * because full-chroma colour does not sit well next to Minecraft's own interface. Text is only ever
 * Minecraft's font at its own size, or a whole multiple of it. Corners are a pixel squircle: the
 * corner pixel of a box is left out. Motion arrives slowly and leaves fast.
 */
public final class Theme {
	// The shared colours live in ui/Palette, which the Field Guide and smithing panel read through ui/style.json.
	public static final int GROUND = Palette.GROUND;
	public static final int PANEL = Palette.PANEL;
	public static final int SUNKEN = Palette.SUNKEN;
	public static final int SUNKEN_LIGHT = Palette.SUNKEN_LIGHT;
	public static final int BORDER = Palette.BORDER;
	public static final int BORDER_CONTROL = Palette.BORDER_CONTROL;
	public static final int BORDER_HOVER = Palette.BORDER_HOVER;

	public static final int TEXT_BRIGHT = Palette.TEXT_BRIGHT;
	public static final int TEXT_BODY = Palette.TEXT_BODY;
	public static final int TEXT_MUTED = Palette.TEXT_MUTED;
	public static final int TEXT_LABEL = Palette.TEXT_LABEL;
	public static final int TEXT_FAINT = Palette.TEXT_FAINT;

	/** The accent: black slabs, set off from the dark panel by a light hairline and light text on top. */
	public static final int ACCENT = Palette.ACCENT;
	public static final int ON_ACCENT = Palette.ON_ACCENT;
	/** Enchantment colours, kept because the charge bars are told apart by them. */
	public static final int LIGHTNING = 0xFFDD1B;
	public static final int SHOCKWAVE = 0x2EE6FF;
	public static final int CHAINS = 0x9FB0BE;
	public static final int ECHO = 0xA98BFF;
	public static final int DANGER = Palette.DANGER;
	/** Enemy health bars, in the red of vanilla's hearts, with the lighter top row its bars carry. */
	public static final int HEALTH = 0xD32020;
	/** The refinement progress bar: emerald green for what is paid, a softer one for what the table would pay. */
	public static final int EMERALD = Palette.EMERALD;
	public static final int EMERALD_SOFT = Palette.EMERALD_SOFT;
	public static final int HEALTH_LIGHT = 0xFF5050;

	public static final float PANEL_ALPHA = 0.85F;

	/** Enter and leave durations: arrivals take their time, departures are immediate. */
	public static final long ENTER_MS = 350;
	public static final long LEAVE_MS = 140;

	private Theme() {
	}

	/** Row colour of a charged perk, by its name; perks without their own colour use the plain text one. */
	public static int perkColor(String perk) {
		return switch (perk) {
			case "lightning" -> LIGHTNING;
			case "shockwave" -> SHOCKWAVE;
			case "chains" -> CHAINS;
			case "echo" -> ECHO;
			default -> TEXT_BRIGHT;
		};
	}

	/** Name colour of an armor trim, so a power reads as the trim it comes from. */
	public static int trimColor(String trim) {
		int rgb = Refinement.trimColor(trim);
		return rgb < 0 ? TEXT_BRIGHT : rgb;
	}

	/** Name colour of the trim that grants {@code perk}. */
	public static int trimColor(Refinement.Perk perk) {
		return trimColor(Refinement.trim(perk));
	}

	public static int argb(float alpha, int rgb) {
		return Math.round(Mth.clamp(alpha, 0.0F, 1.0F) * 255) << 24 | rgb & 0xFFFFFF;
	}

	/** {@code amount} of {@code accent} over {@code base}, the way V/AS derives every tint instead of picking one. */
	public static int mix(int base, int accent, float amount) {
		float t = Mth.clamp(amount, 0.0F, 1.0F);
		int r = Math.round(Mth.lerp(t, base >> 16 & 0xFF, accent >> 16 & 0xFF));
		int g = Math.round(Mth.lerp(t, base >> 8 & 0xFF, accent >> 8 & 0xFF));
		int b = Math.round(Mth.lerp(t, base & 0xFF, accent & 0xFF));
		return r << 16 | g << 8 | b;
	}

	/** A box with its corner pixels left out, the pixel version of the V/AS squircle. */
	public static void box(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int color) {
		if (x1 - x0 < 3 || y1 - y0 < 3) {
			g.fill(x0, y0, x1, y1, color);
			return;
		}
		g.fill(x0 + 1, y0, x1 - 1, y0 + 1, color);
		g.fill(x0, y0 + 1, x1, y1 - 1, color);
		g.fill(x0 + 1, y1 - 1, x1 - 1, y1, color);
	}

	/** A 1px hairline around a box, corners left out to match {@link #box}. */
	public static void hairline(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int color) {
		g.fill(x0 + 1, y0, x1 - 1, y0 + 1, color);
		g.fill(x0 + 1, y1 - 1, x1 - 1, y1, color);
		g.fill(x0, y0 + 1, x0 + 1, y1 - 1, color);
		g.fill(x1 - 1, y0 + 1, x1, y1 - 1, color);
	}

	/** The V/AS panel: translucent near-black fill and one hairline. */
	public static void panel(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, float alpha, int border) {
		box(g, x0, y0, x1, y1, argb(alpha * PANEL_ALPHA, PANEL));
		hairline(g, x0, y0, x1, y1, argb(alpha, border));
	}

	public static float easeOut(float t) {
		float inverse = 1.0F - Mth.clamp(t, 0.0F, 1.0F);
		return 1.0F - inverse * inverse * inverse;
	}

	/** Overshoots a little before settling, for arrivals. */
	public static float easeOutBack(float t) {
		float x = Mth.clamp(t, 0.0F, 1.0F) - 1.0F;
		float overshoot = 1.7F;
		return 1.0F + x * x * ((overshoot + 1.0F) * x + overshoot);
	}

	public static float easeIn(float t) {
		float x = Mth.clamp(t, 0.0F, 1.0F);
		return x * x;
	}
}
