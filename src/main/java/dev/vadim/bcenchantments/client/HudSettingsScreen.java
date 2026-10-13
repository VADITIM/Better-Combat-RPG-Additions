package dev.vadim.bcenchantments.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

/**
 * The settings screen behind Mod Menu's config button, in the {@link Theme} look: one panel on the
 * left with a decoding title and one card per setting, all in Minecraft's own font, and the world left uncovered so the HUD preview
 * on top of it follows every change live. Sliders are drag-only (a click never jumps the value) and
 * come with step and reset buttons. The panel assembles from the top-left when it opens and leaves all
 * at once, faster, when it closes. Nothing is written until Save; Escape and Cancel throw the edits away.
 */
public final class HudSettingsScreen extends Screen {
	private static final int MARGIN = 12;
	private static final int PADDING = 8;
	private static final int CARD_GAP = 4;
	private static final int TITLE_HEIGHT = 24;
	private static final int SLIDER_CARD_HEIGHT = 44;
	private static final int LAYOUT_CARD_HEIGHT = 33;
	private static final int BUTTON_HEIGHT = 16;
	private static final int DRAG_SLOP = 2;
	private static final long STAGGER_MS = 45;
	private static final long LETTER_MS = 35;
	private static final String GLYPHS = "#%&*+=/<>0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

	/** A numeric setting with its range, its step and how its value reads. */
	private record Setting(String key, float min, float max, float step, float fallback,
		Supplier<Float> getter, Consumer<Float> setter, Function<Float, String> format, HudConfig.ChargeLayout only) {
	}

	/** A clickable area found while drawing, used by the mouse handlers on the next event. */
	private record Hit(int x0, int y0, int x1, int y1, Runnable action, Setting slider) {
		boolean contains(double x, double y) {
			return x >= x0 && x < x1 && y >= y0 && y < y1;
		}

		/** Hits are rebuilt every frame, so the same control is recognised by where it sits. */
		boolean sameAs(Hit other) {
			return other != null && other.x0 == x0 && other.y0 == y0 && other.x1 == x1 && other.y1 == y1;
		}
	}

	private final Screen parent;
	private final HudConfig.Values values;
	private final List<Setting> settings = new ArrayList<>();
	private final List<Hit> hits = new ArrayList<>();
	private final Map<String, Float> shownValues = new HashMap<>();

	private long openedAt = -1;
	private long cardsEnterAt = -1;
	private long closingAt = -1;
	private boolean saveOnClose;
	private long lastFrame;

	private Setting dragging;
	private double dragFromX;
	private float dragFromValue;
	private int dragTrackWidth = 1;
	private boolean dragArmed;
	private Hit pressed;

	public HudSettingsScreen(Screen parent) {
		super(Component.translatable("screen.bcenchantments.settings.title"));
		this.parent = parent;
		this.values = HudConfig.get().copy();
		HudConfig.Values defaults = new HudConfig.Values();
		Function<Float, String> pixels = value -> String.format(Locale.ROOT, "%.1f px", value);
		settings.add(new Setting("offset_y", -40.0F, 40.0F, 0.5F, defaults.chargeBarY,
			() -> values.chargeBarY, value -> values.chargeBarY = value, pixels, HudConfig.ChargeLayout.UNDER_CROSSHAIR));
		settings.add(new Setting("distance", 0.0F, 80.0F, 0.5F, defaults.chargeBarSideX,
			() -> values.chargeBarSideX, value -> values.chargeBarSideX = value, pixels, HudConfig.ChargeLayout.BESIDE_CROSSHAIR));
		settings.add(new Setting("offset_y", -40.0F, 40.0F, 0.5F, defaults.chargeBarSideY,
			() -> values.chargeBarSideY, value -> values.chargeBarSideY = value, pixels, HudConfig.ChargeLayout.BESIDE_CROSSHAIR));
		settings.add(new Setting("size", 0.1F, 4.0F, 0.05F, defaults.chargeBarScale,
			() -> values.chargeBarScale, value -> values.chargeBarScale = value, value -> Math.round(value * 100.0F) + "%", null));
	}

	/** Whether this screen is open and drawing the HUD preview itself. */
	public static boolean isOpen(Minecraft minecraft) {
		return minecraft.gui.screen() instanceof HudSettingsScreen;
	}

	@Override
	protected void init() {
		if (openedAt < 0) {
			openedAt = Util.getMillis();
			cardsEnterAt = openedAt;
		}
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		// In game the world stays fully visible for the preview; from the title screen the ground stands in for it.
		if (minecraft.level == null) {
			graphics.fill(0, 0, width, height, Theme.argb(1.0F, Theme.GROUND));
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
		long now = Util.getMillis();
		float dt = Mth.clamp((now - lastFrame) / 1000.0F, 0.0F, 0.1F);
		lastFrame = now;
		hits.clear();

		if (minecraft.level == null) {
			crosshair(g);
		}
		ComboHud.preview(g, values);

		float leave = closingAt < 0 ? 0.0F : Theme.easeIn((now - closingAt) / (float) Theme.LEAVE_MS);
		int panelWidth = Math.max(150, Math.min(176, width / 2 - 2 * MARGIN));
		int x0 = MARGIN;
		int x1 = x0 + panelWidth;
		int inner0 = x0 + PADDING;
		int inner1 = x1 - PADDING;

		List<Setting> shown = new ArrayList<>();
		for (Setting setting : settings) {
			if (setting.only() == null || setting.only() == values.chargeBarLayout) {
				shown.add(setting);
			}
		}
		int contentHeight = TITLE_HEIGHT + 3 * (LAYOUT_CARD_HEIGHT + CARD_GAP) + shown.size() * (SLIDER_CARD_HEIGHT + CARD_GAP) + BUTTON_HEIGHT + CARD_GAP + BUTTON_HEIGHT;
		int y0 = Math.max(MARGIN, (height - contentHeight - 2 * PADDING) / 2);
		int y1 = y0 + contentHeight + 2 * PADDING;

		float panelAlpha = (1.0F - leave) * Theme.easeOut((now - openedAt) / 200.0F);
		g.pose().pushMatrix();
		g.pose().translate(-leave * 6.0F, 0.0F);
		Theme.panel(g, x0, y0, x1, y1, panelAlpha, Theme.BORDER);

		int y = y0 + PADDING;
		int order = 0;
		title(g, now, inner0 + offset(now, openedAt, order), y, enter(now, openedAt, order++) * (1.0F - leave));
		y += TITLE_HEIGHT;

		combatCard(g, now, inner0, y, inner1, mouseX, mouseY, order++, leave);
		y += LAYOUT_CARD_HEIGHT + CARD_GAP;
		particlesCard(g, now, inner0, y, inner1, mouseX, mouseY, order++, leave);
		y += LAYOUT_CARD_HEIGHT + CARD_GAP;
		layoutCard(g, now, inner0, y, inner1, mouseX, mouseY, order++, leave);
		y += LAYOUT_CARD_HEIGHT + CARD_GAP;
		int cardOrder = 0;
		for (Setting setting : shown) {
			sliderCard(g, now, dt, setting, inner0, y, inner1, mouseX, mouseY, cardOrder++, leave);
			y += SLIDER_CARD_HEIGHT + CARD_GAP;
		}
		order += cardOrder;

		forgeButton(g, now, inner0, y, inner1, mouseX, mouseY, order++, leave);
		y += BUTTON_HEIGHT + CARD_GAP;
		footer(g, now, inner0, y, inner1, mouseX, mouseY, order, leave);
		g.pose().popMatrix();
	}

	/** A plain crosshair to place the preview against when there is no world, and so no real one. */
	private void crosshair(GuiGraphicsExtractor g) {
		int cx = width / 2;
		int cy = height / 2;
		int color = Theme.argb(0.8F, Theme.TEXT_BODY);
		g.fill(cx - 4, cy, cx + 5, cy + 1, color);
		g.fill(cx, cy - 4, cx + 1, cy + 5, color);
	}

	/** The title at twice the font's size, each letter decoding out of random glyphs, left to right. */
	private void title(GuiGraphicsExtractor g, long now, float x, int y, float alpha) {
		if (alpha <= 0.0F) {
			return;
		}
		String title = getTitle().getString();
		StringBuilder text = new StringBuilder(title.length());
		long since = now - openedAt;
		for (int i = 0; i < title.length(); i++) {
			char letter = title.charAt(i);
			if (letter == ' ' || since >= 120 + i * LETTER_MS) {
				text.append(letter);
			} else {
				text.append(GLYPHS.charAt(Math.floorMod(now / 40L * 31L + i * 13L, GLYPHS.length())));
			}
		}
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(2.0F, 2.0F);
		g.text(font, text.toString(), 0, 0, Theme.argb(alpha, Theme.TEXT_BRIGHT), true);
		g.pose().popMatrix();
	}

	private void layoutCard(GuiGraphicsExtractor g, long now, int x0, int y, int x1, int mouseX, int mouseY, int order, float leave) {
		float alpha = enter(now, openedAt, order) * (1.0F - leave);
		if (alpha <= 0.0F) {
			return;
		}
		float dx = offset(now, openedAt, order);
		g.pose().pushMatrix();
		g.pose().translate(dx, 0.0F);
		card(g, x0, y, x1, y + LAYOUT_CARD_HEIGHT, alpha, false);
		g.text(font, Component.translatable("screen.bcenchantments.settings.layout"), x0 + 6, y + 5, Theme.argb(alpha, Theme.TEXT_MUTED), false);

		int top = y + 16;
		int middle = (x0 + x1) / 2;
		segment(g, HudConfig.ChargeLayout.UNDER_CROSSHAIR, "screen.bcenchantments.settings.layout.under", x0 + 6, top, middle - 1, mouseX, mouseY, alpha, dx);
		segment(g, HudConfig.ChargeLayout.BESIDE_CROSSHAIR, "screen.bcenchantments.settings.layout.beside", middle + 1, top, x1 - 6, mouseX, mouseY, alpha, dx);
		g.pose().popMatrix();
	}

	/** A two-way switch card: the left half sets the value to true, the right half to false. */
	private void switchCard(GuiGraphicsExtractor g, long now, int x0, int y, int x1, int mouseX, int mouseY, int order, float leave,
		String key, String trueKey, String falseKey, Supplier<Boolean> getter, Consumer<Boolean> setter) {
		float alpha = enter(now, openedAt, order) * (1.0F - leave);
		if (alpha <= 0.0F) {
			return;
		}
		float dx = offset(now, openedAt, order);
		g.pose().pushMatrix();
		g.pose().translate(dx, 0.0F);
		card(g, x0, y, x1, y + LAYOUT_CARD_HEIGHT, alpha, false);
		g.text(font, Component.translatable(key), x0 + 6, y + 5, Theme.argb(alpha, Theme.TEXT_MUTED), false);
		int top = y + 16;
		int middle = (x0 + x1) / 2;
		toggle(g, getter.get(), true, setter, trueKey, x0 + 6, top, middle - 1, mouseX, mouseY, alpha, dx);
		toggle(g, !getter.get(), false, setter, falseKey, middle + 1, top, x1 - 6, mouseX, mouseY, alpha, dx);
		g.pose().popMatrix();
	}

	/** Better Combat on or off, for farms that need vanilla attacks. */
	private void combatCard(GuiGraphicsExtractor g, long now, int x0, int y, int x1, int mouseX, int mouseY, int order, float leave) {
		switchCard(g, now, x0, y, x1, mouseX, mouseY, order, leave, "screen.bcenchantments.settings.better_combat",
			"screen.bcenchantments.settings.on", "screen.bcenchantments.settings.off", () -> values.betterCombat, on -> values.betterCombat = on);
	}

	/** All of the mod's particles, or just a little of each effect. */
	private void particlesCard(GuiGraphicsExtractor g, long now, int x0, int y, int x1, int mouseX, int mouseY, int order, float leave) {
		switchCard(g, now, x0, y, x1, mouseX, mouseY, order, leave, "screen.bcenchantments.settings.particles",
			"screen.bcenchantments.settings.particles.all", "screen.bcenchantments.settings.particles.minimal",
			() -> !values.minimalParticles, all -> values.minimalParticles = !all);
	}

	private void toggle(GuiGraphicsExtractor g, boolean selected, boolean value, Consumer<Boolean> setter, String key,
		int x0, int y0, int x1, int mouseX, int mouseY, float alpha, float dx) {
		int y1 = y0 + 12;
		Hit hit = new Hit(Math.round(x0 + dx), y0, Math.round(x1 + dx), y1, () -> setter.accept(value), null);
		hits.add(hit);
		boolean hovered = hit.contains(mouseX, mouseY);
		int down = hit.sameAs(pressed) ? 1 : 0;
		Theme.box(g, x0, y0 + down, x1, y1, selected ? Theme.argb(alpha, Theme.ACCENT) : Theme.argb(alpha * Theme.PANEL_ALPHA, Theme.SUNKEN));
		int ring = selected ? Theme.TEXT_MUTED : hovered ? Theme.BORDER_HOVER : Theme.BORDER_CONTROL;
		Theme.hairline(g, x0, y0 + down, x1, y1, Theme.argb(alpha, ring));
		String text = Component.translatable(key).getString();
		int color = selected ? Theme.ON_ACCENT : hovered ? Theme.TEXT_BODY : Theme.TEXT_MUTED;
		g.text(font, text, (x0 + x1 - font.width(text)) / 2, y0 + 2 + down, Theme.argb(alpha, color), false);
	}

	/** One half of the layout switch: the selected half is the black slab with light text. */
	private void segment(GuiGraphicsExtractor g, HudConfig.ChargeLayout layout, String key, int x0, int y0, int x1,
		int mouseX, int mouseY, float alpha, float dx) {
		int y1 = y0 + 12;
		boolean selected = values.chargeBarLayout == layout;
		Hit hit = new Hit(Math.round(x0 + dx), y0, Math.round(x1 + dx), y1, () -> {
			if (values.chargeBarLayout != layout) {
				values.chargeBarLayout = layout;
				cardsEnterAt = Util.getMillis();
			}
		}, null);
		hits.add(hit);
		boolean hovered = hit.contains(mouseX, mouseY);
		int down = hit.sameAs(pressed) ? 1 : 0;
		Theme.box(g, x0, y0 + down, x1, y1, selected ? Theme.argb(alpha, Theme.ACCENT) : Theme.argb(alpha * Theme.PANEL_ALPHA, Theme.SUNKEN));
		int ring = selected ? Theme.TEXT_MUTED : hovered ? Theme.BORDER_HOVER : Theme.BORDER_CONTROL;
		Theme.hairline(g, x0, y0 + down, x1, y1, Theme.argb(alpha, ring));
		String text = Component.translatable(key).getString();
		int color = selected ? Theme.ON_ACCENT : hovered ? Theme.TEXT_BODY : Theme.TEXT_MUTED;
		g.text(font, text, (x0 + x1 - font.width(text)) / 2, y0 + 2 + down, Theme.argb(alpha, color), false);
	}

	private void sliderCard(GuiGraphicsExtractor g, long now, float dt, Setting setting, int x0, int y, int x1,
		int mouseX, int mouseY, int order, float leave) {
		long start = Math.max(openedAt + 2 * STAGGER_MS, cardsEnterAt);
		float alpha = enter(now, start, order) * (1.0F - leave);
		if (alpha <= 0.0F) {
			return;
		}
		float dx = offset(now, start, order);
		float value = setting.getter().get();
		String id = setting.key() + setting.only();
		float shownValue = shownValues.getOrDefault(id, value);
		// Eased only while no drag drives it: a value under the cursor must follow the cursor exactly.
		shownValue = dragging == setting ? value : shownValue + (value - shownValue) * Mth.clamp(dt * 14.0F, 0.0F, 1.0F);
		shownValues.put(id, shownValue);

		g.pose().pushMatrix();
		g.pose().translate(dx, 0.0F);
		int y1 = y + SLIDER_CARD_HEIGHT;
		int trackX0 = x0 + 6;
		int trackX1 = x1 - 6;
		int trackY = y + 20;
		Hit track = new Hit(Math.round(trackX0 + dx), trackY - 5, Math.round(trackX1 + dx), trackY + 7, null, setting);
		hits.add(track);
		boolean hovered = track.contains(mouseX, mouseY) || dragging == setting;
		card(g, x0, y, x1, y1, alpha, hovered);

		g.text(font, Component.translatable("screen.bcenchantments.settings." + setting.key()), x0 + 6, y + 5, Theme.argb(alpha, Theme.TEXT_MUTED), false);
		String reading = setting.format().apply(snap(setting, shownValue));
		g.text(font, reading, x1 - 6 - font.width(reading), y + 5, Theme.argb(alpha, Theme.TEXT_BRIGHT), false);

		float fraction = (shownValue - setting.min()) / (setting.max() - setting.min());
		int thumbX = trackX0 + Math.round((trackX1 - trackX0) * Mth.clamp(fraction, 0.0F, 1.0F));
		g.fill(trackX0, trackY, trackX1, trackY + 2, Theme.argb(alpha, Theme.BORDER_CONTROL));
		g.fill(trackX0, trackY, thumbX, trackY + 2, Theme.argb(alpha, Theme.ACCENT));
		int thumbHalf = dragging == setting ? 3 : 2;
		Theme.box(g, thumbX - thumbHalf, trackY - 4, thumbX + thumbHalf + 1, trackY + 6, Theme.argb(alpha, Theme.ACCENT));
		Theme.hairline(g, thumbX - thumbHalf, trackY - 4, thumbX + thumbHalf + 1, trackY + 6, Theme.argb(alpha, hovered ? Theme.TEXT_BRIGHT : Theme.TEXT_MUTED));
		if (dragging == setting) {
			dragTrackWidth = Math.max(1, trackX1 - trackX0);
		}

		int buttonY = y + 28;
		String reset = Component.translatable("screen.bcenchantments.settings.reset").getString();
		int resetX0 = x1 - 6 - font.width(reset) - 10;
		button(g, "-", x0 + 6, buttonY, x0 + 6 + 14, buttonY + 11, mouseX, mouseY, alpha, dx, () -> nudge(setting, -1));
		button(g, "+", x0 + 22, buttonY, x0 + 22 + 14, buttonY + 11, mouseX, mouseY, alpha, dx, () -> nudge(setting, 1));
		button(g, reset, resetX0, buttonY, x1 - 6, buttonY + 11, mouseX, mouseY, alpha, dx, () -> setting.setter().accept(setting.fallback()));
		g.pose().popMatrix();
	}

	private void footer(GuiGraphicsExtractor g, long now, int x0, int y, int x1, int mouseX, int mouseY, int order, float leave) {
		float alpha = enter(now, openedAt, order) * (1.0F - leave);
		if (alpha <= 0.0F) {
			return;
		}
		float dx = offset(now, openedAt, order);
		g.pose().pushMatrix();
		g.pose().translate(dx, 0.0F);
		int middle = (x0 + x1) / 2;
		button(g, Component.translatable("screen.bcenchantments.settings.cancel").getString(), x0, y, middle - 2, y + BUTTON_HEIGHT,
			mouseX, mouseY, alpha, dx, () -> close(false));

		Hit save = new Hit(Math.round(middle + 2 + dx), y, Math.round(x1 + dx), y + BUTTON_HEIGHT, () -> close(true), null);
		hits.add(save);
		boolean hovered = save.contains(mouseX, mouseY);
		int down = save.sameAs(pressed) ? 1 : 0;
		// The one filled slab on the panel: saving is the action this screen exists for.
		Theme.box(g, middle + 2, y + down, x1, y + BUTTON_HEIGHT, Theme.argb(alpha, Theme.ACCENT));
		Theme.hairline(g, middle + 2, y + down, x1, y + BUTTON_HEIGHT, Theme.argb(alpha, hovered ? Theme.TEXT_BRIGHT : Theme.TEXT_MUTED));
		String text = Component.translatable("screen.bcenchantments.settings.save").getString();
		g.text(font, text, (middle + 2 + x1 - font.width(text)) / 2, y + 4 + down, Theme.argb(alpha, Theme.ON_ACCENT), false);
		g.pose().popMatrix();
	}

	private void forgeButton(GuiGraphicsExtractor g, long now, int x0, int y, int x1, int mouseX, int mouseY, int order, float leave) {
		float alpha = enter(now, openedAt, order) * (1.0F - leave);
		if (alpha <= 0.0F) {
			return;
		}
		float dx = offset(now, openedAt, order);
		g.pose().pushMatrix();
		g.pose().translate(dx, 0.0F);
		button(g, Component.translatable("screen.bcenchantments.settings.forge").getString(), x0, y, x1, y + BUTTON_HEIGHT,
			mouseX, mouseY, alpha, dx, () -> minecraft.gui.setScreen(new ForgeScreen(this)));
		g.pose().popMatrix();
	}

	/** An outlined button: the ring lightens on hover and the button sinks a pixel while pressed. */
	private void button(GuiGraphicsExtractor g, String text, int x0, int y0, int x1, int y1, int mouseX, int mouseY,
		float alpha, float dx, Runnable action) {
		Hit hit = new Hit(Math.round(x0 + dx), y0, Math.round(x1 + dx), y1, action, null);
		hits.add(hit);
		boolean hovered = hit.contains(mouseX, mouseY);
		int down = hit.sameAs(pressed) ? 1 : 0;
		Theme.box(g, x0, y0 + down, x1, y1, Theme.argb(alpha * Theme.PANEL_ALPHA, Theme.SUNKEN));
		Theme.hairline(g, x0, y0 + down, x1, y1, Theme.argb(alpha, hovered ? Theme.BORDER_HOVER : Theme.BORDER_CONTROL));
		g.text(font, text, (x0 + x1 - font.width(text) + 1) / 2, (y0 + y1) / 2 - 4 + down, Theme.argb(alpha, hovered ? Theme.TEXT_BRIGHT : Theme.TEXT_BODY), false);
	}

	private void card(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, float alpha, boolean hovered) {
		Theme.box(g, x0, y0, x1, y1, Theme.argb(alpha * Theme.PANEL_ALPHA, Theme.SUNKEN_LIGHT));
		Theme.hairline(g, x0, y0, x1, y1, Theme.argb(alpha, hovered ? Theme.BORDER_HOVER : Theme.BORDER));
	}

	/** Opacity of the {@code order}th element of a group that started arriving at {@code start}. */
	private static float enter(long now, long start, int order) {
		return Theme.easeOut((now - start - order * STAGGER_MS) / (float) Theme.ENTER_MS);
	}

	/** Sideways slide of an arriving element: from the left, overshooting a touch before it settles. */
	private static float offset(long now, long start, int order) {
		return (Theme.easeOutBack((now - start - order * STAGGER_MS) / (float) Theme.ENTER_MS) - 1.0F) * 10.0F;
	}

	private static float snap(Setting setting, float value) {
		float snapped = Math.round(value / setting.step()) * setting.step();
		return Mth.clamp(snapped, setting.min(), setting.max());
	}

	private void nudge(Setting setting, int steps) {
		setting.setter().accept(snap(setting, setting.getter().get() + steps * setting.step()));
	}

	private void close(boolean save) {
		if (closingAt < 0) {
			closingAt = Util.getMillis();
			saveOnClose = save;
			dragging = null;
			pressed = null;
		}
	}

	@Override
	public void tick() {
		if (closingAt >= 0 && Util.getMillis() - closingAt >= Theme.LEAVE_MS) {
			closingAt = Long.MAX_VALUE;
			if (saveOnClose) {
				HudConfig.save(values);
			}
			minecraft.gui.setScreen(parent);
		}
	}

	@Override
	public void onClose() {
		close(false);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
			close(true);
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (closingAt >= 0 || event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
			return true;
		}
		for (Hit hit : hits) {
			if (hit.contains(event.x(), event.y())) {
				if (hit.slider() != null) {
					dragging = hit.slider();
					dragFromX = event.x();
					dragFromValue = dragging.getter().get();
					dragArmed = false;
				} else {
					pressed = hit;
				}
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (dragging == null) {
			return super.mouseDragged(event, dx, dy);
		}
		double travelled = event.x() - dragFromX;
		// Under the slop this is still a click, and a click never moves the value.
		if (!dragArmed && Math.abs(travelled) < DRAG_SLOP) {
			return true;
		}
		dragArmed = true;
		float range = dragging.max() - dragging.min();
		dragging.setter().accept(snap(dragging, dragFromValue + (float) (travelled / dragTrackWidth) * range));
		return true;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (dragging != null) {
			dragging = null;
			return true;
		}
		if (pressed != null) {
			Hit released = pressed;
			pressed = null;
			for (Hit hit : hits) {
				if (hit.contains(event.x(), event.y()) && hit.sameAs(released)) {
					hit.action().run();
					break;
				}
			}
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		for (Hit hit : hits) {
			if (hit.slider() != null && hit.contains(x, y) && scrollY != 0.0) {
				nudge(hit.slider(), scrollY > 0.0 ? 1 : -1);
				return true;
			}
		}
		return super.mouseScrolled(x, y, scrollX, scrollY);
	}
}
