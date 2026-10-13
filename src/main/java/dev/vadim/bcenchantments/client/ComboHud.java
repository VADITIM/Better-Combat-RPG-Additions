package dev.vadim.bcenchantments.client;

import dev.vadim.bcenchantments.network.ComboHudPayload;
import dev.vadim.bcenchantments.perk.ChargedPerk;
import dev.vadim.bcenchantments.perk.Perks;
import java.util.List;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.HumanoidArm;

/**
 * Combat HUD in the {@link Theme} look. The combo count stands alone at the right edge of the screen,
 * with no backdrop: it pops in when a combo starts, pops bigger when it rises, shrinks toward the
 * bottom centre of itself as Better Combat's combo window runs out, and pops out when the combo ends,
 * shaking red first if it broke on a miss. Each hand's charged perks show as rows of small
 * rounded blocks, one per hit they need, either under the crosshair or beside it with the main hand's
 * weapon on the main arm's side. Blocks arrive one after another and pop as they fill; when an
 * enchantment fires, or the rows are no longer needed, they snap away one after another instead of
 * fading.
 */
public final class ComboHud implements HudElement {
	private static final int MARGIN = 12;
	/** Height of the count's bottom edge above the screen's middle. */
	private static final int COUNT_BOTTOM = 0;
	private static final float COUNT_SCALE = 2.0F;
	/** Size the count shrinks to, as a share of its full size, right before the combo drops. */
	private static final float COUNT_SMALLEST = 0.55F;
	private static final long POP_MS = 260;
	private static final long DECODE_MS = 160;
	private static final long BREAK_MS = 600;
	private static final long POP_OUT_MS = 160;
	/** Timeline length when Better Combat's own timer cannot be read. */
	private static final long FALLBACK_WINDOW_MS = 3000;

	/** Charge bar blocks and spacing at scale 1; position and scale come from {@link HudConfig}. */
	private static final int SEGMENT_WIDTH = 4;
	private static final int SEGMENT_HEIGHT = 1;
	private static final int SEGMENT_GAP = 1;
	private static final int ROW_GAP = 1;
	private static final int HAND_GAP = 3;
	private static final long STAGGER_MS = 30;
	private static final long BLOCK_ENTER_MS = 180;
	private static final long LIT_POP_MS = 160;
	/** A fired row holds full and white for a moment, then its blocks snap away one after another and come back empty. */
	private static final long FIRE_HOLD_MS = 120;
	private static final long EXIT_STAGGER_MS = 25;
	private static final long EXIT_MS = 90;

	/** One charged perk's row of blocks. */
	private static final class Row {
		final ChargedPerk perk;
		final int color;
		int segments;
		int value;
		float shown;
		int lit;
		long litAt = -1;
		long firedAt = -1;

		Row(ChargedPerk perk) {
			this.perk = perk;
			this.color = Theme.perkColor(perk.name());
		}

		void set(int value, int segments, boolean fired, long now) {
			this.value = value;
			this.segments = segments;
			if (fired) {
				// The charge is spent by the exit animation, not by easing the fill back down.
				firedAt = now;
				shown = 0.0F;
				lit = 0;
			}
		}

		void advance(float dt, long now) {
			shown = approach(shown, segments > 0 ? value / (float) segments : 0.0F, dt * 5.0F);
			int litNow = (int) Math.floor(shown * segments + 0.001F);
			if (litNow > lit) {
				litAt = now;
			}
			lit = litNow;
		}

		boolean firing(long now) {
			return firedAt >= 0 && now - firedAt < fireDuration(segments);
		}
	}

	/** One hand's rows and whether they are arriving, standing or leaving. */
	private static final class Hand {
		final Row[] rows = Perks.CHARGED.stream().map(Row::new).toArray(Row[]::new);
		boolean visible;
		long enterAt = -1;
		long leaveAt = -1;

		void set(List<ComboHudPayload.Charge> charges, long now) {
			for (int i = 0; i < rows.length; i++) {
				ComboHudPayload.Charge charge = i < charges.size() ? charges.get(i) : ComboHudPayload.Charge.NONE;
				rows[i].set(charge.value(), charge.max(), charge.fired(), now);
			}
		}

		boolean hasRows() {
			return shownRows() > 0;
		}

		int shownRows() {
			int shown = 0;
			for (Row row : rows) {
				if (row.segments > 0) {
					shown++;
				}
			}
			return shown;
		}

		int height() {
			int shown = shownRows();
			return shown == 0 ? 0 : shown * SEGMENT_HEIGHT + (shown - 1) * ROW_GAP;
		}

		/** Whether any row still holds charge or is playing its fire animation. */
		boolean charged(long now) {
			for (Row row : rows) {
				if (row.value > 0 || row.firing(now)) {
					return true;
				}
			}
			return false;
		}

		/** Empties the rows whose charge {@code drops} says is gone. */
		void drop(Predicate<ChargedPerk> drops) {
			for (Row row : rows) {
				if (drops.test(row.perk)) {
					row.value = 0;
				}
			}
		}

		/** Updates whether the rows should be up and returns whether anything is left to draw. */
		boolean update(boolean wanted, long now) {
			if (wanted && !visible) {
				visible = true;
				enterAt = now;
			} else if (!wanted && visible) {
				visible = false;
				leaveAt = now;
			}
			int segments = 0;
			for (Row row : rows) {
				segments = Math.max(segments, row.segments);
			}
			return visible || leaveAt >= 0 && now - leaveAt < exitDuration(segments);
		}

		long exitAt() {
			return visible ? -1 : leaveAt;
		}
	}

	private static final Hand MAIN_HAND = new Hand();
	private static final Hand OFF_HAND = new Hand();

	private static int combo;
	private static int brokenCombo;
	private static long updatedAt;
	private static long poppedAt = -1;
	private static long brokeAt = -1;
	private static boolean missed;
	private static boolean meterVisible;
	private static long meterEnterAt = -1;
	private static long meterLeaveAt = -1;

	private static float shownTimer;
	private static long lastFrame;

	public static void update(ComboHudPayload payload) {
		long now = Util.getMillis();
		if (payload.combo() == 0 && combo > 0) {
			breakCombo(now, true);
		} else if (payload.combo() > combo || payload.combo() == 1) {
			poppedAt = now;
			brokeAt = -1;
		}
		combo = payload.combo();
		MAIN_HAND.set(payload.mainHand(), now);
		OFF_HAND.set(payload.offHand(), now);
		updatedAt = now;
	}

	/** Whether the player's combo is running, which keeps enemies' combo damage trails up. */
	public static boolean comboActive() {
		return combo > 0;
	}

	/** Ends the combo; {@code miss} when a swing missed rather than the window running out. */
	private static void breakCombo(long now, boolean miss) {
		brokenCombo = combo;
		brokeAt = now;
		missed = miss;
		combo = 0;
		// Other perks keep their charge past the combo until their own time runs out.
		for (Hand hand : new Hand[] {MAIN_HAND, OFF_HAND}) {
			hand.drop(perk -> perk.keepTicks(0) == ChargedPerk.BREAKS_WITH_COMBO);
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		// The settings screen draws its own preview on top; the real HUD would only double it.
		if (mc.player == null || HudSettingsScreen.isOpen(mc)) {
			return;
		}
		long now = Util.getMillis();
		float dt = Mth.clamp((now - lastFrame) / 1000.0F, 0.0F, 0.1F);
		lastFrame = now;

		float timer = timer(delta, now);
		// Better Combat drops the combo on its own once the window runs out, so the meter does too.
		if (combo > 0 && timer <= 0.0F) {
			breakCombo(now, false);
		}
		for (Hand hand : new Hand[] {MAIN_HAND, OFF_HAND}) {
			hand.drop(perk -> expired(perk, now));
			for (Row row : hand.rows) {
				row.advance(dt, now);
			}
		}
		shownTimer = combo > 0 ? (shownTimer < timer ? approach(shownTimer, timer, dt * 8.0F) : timer) : approach(shownTimer, 0.0F, dt * 4.0F);

		// A miss shakes red before the count pops out; a combo that ran out pops out straight away.
		boolean shaking = brokeAt >= 0 && missed && now - brokeAt < BREAK_MS;
		boolean wanted = combo >= 2 || shaking;
		if (wanted && !meterVisible) {
			meterVisible = true;
			meterEnterAt = now;
		} else if (!wanted && meterVisible) {
			meterVisible = false;
			meterLeaveAt = now;
		}
		if (meterVisible) {
			meter(g, mc.font, now, (now - meterEnterAt) / (float) Theme.ENTER_MS, 0.0F,
				shaking ? brokenCombo : combo, shaking, shaking, shaking ? 1.0F : shownTimer);
		} else if (meterLeaveAt >= 0 && now - meterLeaveAt < POP_OUT_MS) {
			boolean broken = brokeAt >= 0 && combo == 0;
			meter(g, mc.font, now, 1.0F, (now - meterLeaveAt) / (float) POP_OUT_MS,
				broken ? brokenCombo : Math.max(combo, 2), broken && missed, false, missed ? 1.0F : shownTimer);
		}

		boolean drawMain = MAIN_HAND.update(MAIN_HAND.hasRows() && charging(MAIN_HAND, now), now);
		boolean drawOff = OFF_HAND.update(OFF_HAND.hasRows() && charging(OFF_HAND, now), now);
		if (drawMain || drawOff) {
			charges(g, now, HudConfig.get(), MAIN_HAND, drawMain, OFF_HAND, drawOff, rightHanded(mc));
		}
	}

	private static boolean charging(Hand hand, long now) {
		return combo > 0 || hand.charged(now);
	}

	private static boolean rightHanded(Minecraft mc) {
		return mc.options.mainHand().get() == HumanoidArm.RIGHT;
	}

	/**
	 * Sample HUD for the settings screen's live preview: a combo count running down and both hands'
	 * bars filling one block at a time and firing, placed by the given settings rather than the saved ones.
	 */
	public static void preview(GuiGraphicsExtractor g, HudConfig.Values values) {
		Minecraft mc = Minecraft.getInstance();
		long now = Util.getMillis();
		float timer = 1.0F - (now % 2500L) / 2500.0F;
		meter(g, mc.font, now, 1.0F, 0.0F, 7, false, false, timer);

		Hand main = new Hand();
		for (int i = 0; i < main.rows.length; i++) {
			sample(main.rows[i], Math.max(2, 4 - i), 650L + 150L * i, now);
		}
		Hand off = new Hand();
		if (off.rows.length > 0) {
			sample(off.rows[0], 4, 750L, now);
		}
		charges(g, now, values, main, true, off, true, rightHanded(mc));
	}

	/** Fills {@code row} one block per {@code stepMs}, firing when it wraps. */
	private static void sample(Row row, int segments, long stepMs, long now) {
		long cycle = stepMs * (segments + 1);
		int step = (int) (now % cycle / stepMs);
		row.segments = segments;
		row.value = step;
		row.shown = step / (float) segments;
		row.lit = step;
		row.litAt = step > 0 ? now - now % stepMs : -1;
		row.firedAt = now - now % cycle;
	}

	/**
	 * The combo count, scaled about the bottom centre of itself: {@code enter} runs 0 to 1 as it pops
	 * in, {@code leave} 0 to 1 as it pops out, and {@code timer} shrinks it as the combo window drains.
	 */
	private static void meter(GuiGraphicsExtractor g, Font font, long now, float enter, float leave,
		int shown, boolean broken, boolean shaking, float timer) {
		float alpha = Mth.clamp(enter * 3.0F, 0.0F, 1.0F) * (1.0F - Theme.easeIn(leave));
		if (alpha <= 0.02F) {
			return;
		}
		float pop = poppedAt < 0 ? 0.0F : 1.0F - Mth.clamp((now - poppedAt) / (float) POP_MS, 0.0F, 1.0F);
		float scale = COUNT_SCALE
			* Theme.easeOutBack(enter)
			* (COUNT_SMALLEST + (1.0F - COUNT_SMALLEST) * Mth.clamp(timer, 0.0F, 1.0F))
			* (1.0F + 0.4F * pop * pop)
			* (1.0F + 0.6F * Theme.easeOut(leave));
		if (scale <= 0.01F) {
			return;
		}
		float shake = shaking ? Mth.sin(now * 0.09F) * 3.0F : 0.0F;
		String count = "x" + (poppedAt >= 0 && now - poppedAt < DECODE_MS && !broken ? decode(shown, now) : String.valueOf(shown));
		// Anchored where the count stands at full size, so shrinking pulls it toward its own bottom centre.
		float anchorX = g.guiWidth() - MARGIN - font.width(count) * COUNT_SCALE / 2.0F;
		float anchorY = g.guiHeight() / 2.0F + COUNT_BOTTOM;
		g.pose().pushMatrix();
		g.pose().translate(anchorX + shake, anchorY);
		g.pose().scale(scale, scale);
		g.text(font, count, -font.width(count) / 2, -8, Theme.argb(alpha, broken ? Theme.DANGER : Theme.TEXT_BRIGHT), true);
		g.pose().popMatrix();
	}

	/** The count with every digit swapped for a random one, the ASCII pinch of the decode. */
	private static String decode(int value, long now) {
		String digits = String.valueOf(value);
		StringBuilder scrambled = new StringBuilder(digits.length());
		long seed = now / 40L;
		for (int i = 0; i < digits.length(); i++) {
			scrambled.append((char) ('0' + Math.floorMod(seed * 31L + i * 17L + 7L, 10L)));
		}
		return scrambled.toString();
	}

	/** Both hands' rows, under the crosshair or beside it. */
	private static void charges(GuiGraphicsExtractor g, long now, HudConfig.Values values, Hand main, boolean drawMain,
		Hand off, boolean drawOff, boolean rightHanded) {
		float scale = Math.max(0.05F, values.chargeBarScale);
		// Blocks are drawn in screen pixels so their corners can be rounded by a whole pixel.
		int pixels = Math.max(1, Math.round(Minecraft.getInstance().getWindow().getGuiScale() * scale));
		float centerX = g.guiWidth() / 2.0F;
		float centerY = g.guiHeight() / 2.0F;
		if (values.chargeBarLayout == HudConfig.ChargeLayout.BESIDE_CROSSHAIR) {
			int mainSide = rightHanded ? 1 : -1;
			if (drawMain) {
				side(g, now, main, centerX + mainSide * values.chargeBarSideX, centerY + values.chargeBarSideY, scale, pixels, mainSide);
			}
			if (drawOff) {
				side(g, now, off, centerX - mainSide * values.chargeBarSideX, centerY + values.chargeBarSideY, scale, pixels, -mainSide);
			}
			return;
		}
		g.pose().pushMatrix();
		g.pose().translate(centerX, centerY + values.chargeBarY);
		g.pose().scale(scale, scale);
		int y = 0;
		if (drawMain && main.hasRows()) {
			y = rows(g, now, main, 0, y, pixels, 0) + HAND_GAP - ROW_GAP;
		}
		if (drawOff && off.hasRows()) {
			rows(g, now, off, 0, y, pixels, 0);
		}
		g.pose().popMatrix();
	}

	/** One hand's rows beside the crosshair, growing away from it, centred on {@code y}. */
	private static void side(GuiGraphicsExtractor g, long now, Hand hand, float x, float y, float scale, int pixels, int direction) {
		if (!hand.hasRows()) {
			return;
		}
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(scale, scale);
		rows(g, now, hand, 0, -hand.height() / 2, pixels, direction);
		g.pose().popMatrix();
	}

	/** Draws a hand's rows from {@code y} down and returns the y under the last one. */
	private static int rows(GuiGraphicsExtractor g, long now, Hand hand, int x, int y, int pixels, int direction) {
		for (Row row : hand.rows) {
			if (row.segments > 0) {
				bar(g, now, row, hand.visible ? hand.enterAt : -1, hand.exitAt(), x, y, pixels, direction);
				y += SEGMENT_HEIGHT + ROW_GAP;
			}
		}
		return y;
	}

	/**
	 * Row of separate blocks: centred on {@code x} when {@code direction} is 0, otherwise starting at
	 * {@code x} and growing right (1) or left (-1). Blocks arrive one after another from the start of
	 * the row, fade to their colour as the fill reaches them and pop when they light. When the row
	 * fires, or the whole hand leaves at {@code exitAt}, the blocks snap away one after another.
	 */
	private static void bar(GuiGraphicsExtractor g, long now, Row row, long enterAt, long exitAt, int x, int y, int pixels, int direction) {
		int segments = row.segments;
		float width = segments * SEGMENT_WIDTH + (segments - 1) * SEGMENT_GAP;
		long sinceFired = row.firing(now) ? now - row.firedAt : -1;
		for (int i = 0; i < segments; i++) {
			float x0;
			if (direction == 0) {
				x0 = x - width / 2.0F + i * (SEGMENT_WIDTH + SEGMENT_GAP);
			} else if (direction > 0) {
				x0 = x + i * (SEGMENT_WIDTH + SEGMENT_GAP);
			} else {
				x0 = x - (i + 1) * SEGMENT_WIDTH - i * SEGMENT_GAP;
			}
			// Beside the crosshair block 0 is the inner one, so those rows arrive, fill and leave outward.
			float size = enterAt < 0 ? 1.0F : Theme.easeOutBack((now - enterAt - i * STAGGER_MS) / (float) BLOCK_ENTER_MS);
			if (exitAt >= 0) {
				size *= 1.0F - Theme.easeIn((now - exitAt - i * EXIT_STAGGER_MS) / (float) EXIT_MS);
			}
			float part = Mth.clamp(row.shown * segments - i, 0.0F, 1.0F);
			float white = 0.0F;
			if (sinceFired >= 0) {
				long exitStart = FIRE_HOLD_MS + i * EXIT_STAGGER_MS;
				long returnStart = FIRE_HOLD_MS + (segments - 1) * EXIT_STAGGER_MS + EXIT_MS + i * STAGGER_MS;
				if (sinceFired < exitStart + EXIT_MS) {
					// Spent: full and flashing white, then snapping away.
					part = 1.0F;
					white = 1.0F - Mth.clamp(sinceFired / (float) FIRE_HOLD_MS, 0.0F, 1.0F) * 0.6F;
					size *= 1.0F - Theme.easeIn((sinceFired - exitStart) / (float) EXIT_MS);
				} else {
					// Back, empty, arriving the way the row first did.
					size *= Theme.easeOutBack((sinceFired - returnStart) / (float) BLOCK_ENTER_MS);
				}
			}
			if (size <= 0.01F) {
				continue;
			}
			float pop = i == row.lit - 1 && row.litAt >= 0 && now - row.litAt < LIT_POP_MS ? 1.0F - (now - row.litAt) / (float) LIT_POP_MS : 0.0F;

			g.pose().pushMatrix();
			g.pose().translate(x0 + SEGMENT_WIDTH / 2.0F, y + SEGMENT_HEIGHT / 2.0F);
			g.pose().scale(size * (1.0F + 0.25F * pop) / pixels, size * (1.0F + 1.0F * pop) / pixels);
			int halfWidth = SEGMENT_WIDTH * pixels / 2;
			int top = -(SEGMENT_HEIGHT * pixels) / 2;
			int bottom = top + SEGMENT_HEIGHT * pixels;
			if (part < 1.0F) {
				Theme.box(g, -halfWidth, top, halfWidth, bottom, Theme.argb(0.18F * (1.0F - part), 0x000000));
			}
			if (part > 0.0F) {
				Theme.box(g, -halfWidth, top, halfWidth, bottom, Theme.argb(part, Theme.mix(row.color, 0xFFFFFF, white)));
			}
			g.pose().popMatrix();
		}
	}

	/** How long a fired row takes to flash, snap away and come back empty. */
	private static long fireDuration(int segments) {
		return FIRE_HOLD_MS + exitDuration(segments) + Math.max(0, segments - 1) * STAGGER_MS + BLOCK_ENTER_MS;
	}

	/** How long a row of {@code segments} blocks takes to snap away. */
	private static long exitDuration(int segments) {
		return Math.max(0, segments - 1) * EXIT_STAGGER_MS + EXIT_MS;
	}

	/** Fraction of the combo window left, 1 right after a swing starts and 0 when the combo drops. */
	private static float timer(DeltaTracker delta, long now) {
		ComboClock.Source clock = ComboClock.source;
		if (clock != null && clock.bcenchantments$comboReset() > 0) {
			float elapsed = clock.bcenchantments$lastAttacked() + delta.getGameTimeDeltaPartialTick(false);
			return Mth.clamp(1.0F - elapsed / clock.bcenchantments$comboReset(), 0.0F, 1.0F);
		}
		return Mth.clamp(1.0F - (now - updatedAt) / (float) FALLBACK_WINDOW_MS, 0.0F, 1.0F);
	}

	/** Whether {@code perk}'s charge has outlived its time since the last swing; never for one that breaks with the combo. */
	private static boolean expired(ChargedPerk perk, long now) {
		ComboClock.Source clock = ComboClock.source;
		if (clock != null && clock.bcenchantments$comboReset() > 0) {
			int keep = perk.keepTicks(clock.bcenchantments$comboReset());
			return keep != ChargedPerk.BREAKS_WITH_COMBO && clock.bcenchantments$lastAttacked() > keep;
		}
		int keep = perk.keepTicks((int) (FALLBACK_WINDOW_MS / 50L));
		return keep != ChargedPerk.BREAKS_WITH_COMBO && now - updatedAt > keep * 50L;
	}

	private static float approach(float value, float target, float step) {
		return value < target ? Math.min(target, value + step) : Math.max(target, value - step);
	}
}
