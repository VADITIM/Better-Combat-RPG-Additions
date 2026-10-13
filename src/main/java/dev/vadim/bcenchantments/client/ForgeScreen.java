package dev.vadim.bcenchantments.client;

import dev.vadim.bcenchantments.network.ForgePayload;
import dev.vadim.bcenchantments.perk.Synergy;
import dev.vadim.bcenchantments.progression.Refinement;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * The weapon forge, opened from the settings screen: pick a weapon and the trim powers it should carry,
 * and the server hands over the finished weapon, so a combination can be tried without refining a weapon
 * and crafting trims first. A power the weapon cannot take is locked out and says why when hovered: the
 * Uppercut needs a sword, spear or mace, and a shield takes one of its few powers. Click a power to
 * raise it one level, past level III to take it off again (right click takes it off at once). Powers
 * combine in pairs in the order they were chosen, as at the smithing table; the right panel shows the
 * pairs that makes.
 */
public final class ForgeScreen extends Screen {
	private static final int MARGIN = 12;
	private static final int PADDING = 8;
	private static final int GAP = 4;
	private static final int ROW = 12;
	private static final int LEFT_WIDTH = 176;
	private static final int RIGHT_WIDTH = 150;
	private static final String[] NUMERALS = {"", "I", "II", "III"};

	/** A clickable area found while drawing; the action gets the mouse button. */
	private record Hit(int x0, int y0, int x1, int y1, IntConsumer action, @Nullable Component lockedReason) {
		boolean contains(double x, double y) {
			return x >= x0 && x < x1 && y >= y0 && y < y1;
		}
	}

	private final Screen parent;
	private final List<Item> weapons = new ArrayList<>();
	private final List<Refinement.Power> chosen = new ArrayList<>();
	private final List<Hit> hits = new ArrayList<>();
	private int weapon;
	private long openedAt = -1;
	private Component status = Component.empty();

	public ForgeScreen(Screen parent) {
		super(Component.translatable("screen.bcenchantments.forge.title"));
		this.parent = parent;
		for (Item item : BuiltInRegistries.ITEM) {
			if (Refinement.isBase(new ItemStack(item))) {
				weapons.add(item);
			}
		}
	}

	@Override
	protected void init() {
		if (openedAt < 0) {
			openedAt = Util.getMillis();
		}
	}

	private ItemStack base() {
		return weapons.isEmpty() ? ItemStack.EMPTY : new ItemStack(weapons.get(weapon));
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		graphics.fill(0, 0, width, height, Theme.argb(minecraft.level == null ? 1.0F : 0.55F, Theme.GROUND));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
		hits.clear();
		float alpha = Theme.easeOut((Util.getMillis() - openedAt) / (float) Theme.ENTER_MS);
		ItemStack base = base();
		List<Refinement.Perk> perks = new ArrayList<>();
		for (Refinement.Perk perk : Refinement.Perk.values()) {
			if (perk.built()) {
				perks.add(perk);
			}
		}

		int contentHeight = 24 + ROW + 6 + GAP + 12 + (perks.size() + 1) / 2 * (ROW + 2) + GAP + 12 + GAP + ROW;
		int top = Math.max(MARGIN, (height - contentHeight - 2 * PADDING) / 2);
		int bottom = top + contentHeight + 2 * PADDING;
		int left = Math.max(MARGIN, (width - (LEFT_WIDTH + GAP + RIGHT_WIDTH)) / 2);
		int middle = left + LEFT_WIDTH;
		int right = middle + GAP + RIGHT_WIDTH;

		Theme.panel(g, left, top, middle, bottom, alpha, Theme.BORDER);
		Theme.panel(g, middle + GAP, top, right, bottom, alpha, Theme.BORDER);

		int x0 = left + PADDING;
		int x1 = middle - PADDING;
		int y = top + PADDING;
		g.pose().pushMatrix();
		g.pose().translate(x0, y);
		g.pose().scale(2.0F, 2.0F);
		g.text(font, getTitle(), 0, 0, Theme.argb(alpha, Theme.TEXT_BRIGHT), true);
		g.pose().popMatrix();
		y += 24;

		// Weapon picker: arrows either side of the icon and name.
		button(g, "<", x0, y, x0 + 14, y + ROW, mouseX, mouseY, alpha, click -> pick(-1), null);
		button(g, ">", x1 - 14, y, x1, y + ROW, mouseX, mouseY, alpha, click -> pick(1), null);
		Component name = base.isEmpty() ? Component.empty() : base.getHoverName();
		g.item(base, x0 + 18, y - 2);
		g.text(font, name, x0 + 38, y + 2, Theme.argb(alpha, Theme.TEXT_BRIGHT), false);
		y += ROW + 6 + GAP;

		g.text(font, Component.translatable("screen.bcenchantments.forge.powers"), x0, y, Theme.argb(alpha, Theme.TEXT_MUTED), false);
		y += 12;
		Component hoveredReason = null;
		int columnWidth = (x1 - x0 - GAP) / 2;
		for (int i = 0; i < perks.size(); i++) {
			Refinement.Perk perk = perks.get(i);
			int cx0 = x0 + i % 2 * (columnWidth + GAP);
			int cy0 = y + i / 2 * (ROW + 2);
			String reason = Refinement.lockedReason(perk, base, chosen);
			Component locked = reason == null ? null : Component.translatable(reason);
			int index = indexOf(perk);
			boolean hovered = mouseX >= cx0 && mouseX < cx0 + columnWidth && mouseY >= cy0 && mouseY < cy0 + ROW;
			if (hovered && locked != null) {
				hoveredReason = locked;
			}
			chip(g, perk, index < 0 ? 0 : chosen.get(index).level(), locked != null, cx0, cy0, cx0 + columnWidth, cy0 + ROW, hovered, alpha);
			hits.add(new Hit(cx0, cy0, cx0 + columnWidth, cy0 + ROW, button -> click(perk, button), locked));
		}
		y += (perks.size() + 1) / 2 * (ROW + 2) + GAP;

		Component line = hoveredReason != null ? hoveredReason : status;
		int lineColor = hoveredReason != null ? Theme.DANGER : Theme.TEXT_MUTED;
		g.text(font, line, x0, y, Theme.argb(alpha, lineColor), false);
		y += 12 + GAP;

		int halfway = (x0 + x1) / 2;
		button(g, Component.translatable("screen.bcenchantments.forge.back").getString(), x0, y, halfway - 2, y + ROW + 4,
			mouseX, mouseY, alpha, click -> onClose(), null);
		forgeButton(g, halfway + 2, y, x1, y + ROW + 4, mouseX, mouseY, alpha);

		pairs(g, middle + GAP + PADDING, top + PADDING, right - PADDING, alpha);
	}

	/** The right panel: the pairs the chosen powers make, in order, with the combination each one has. */
	private void pairs(GuiGraphicsExtractor g, int x0, int y, int x1, float alpha) {
		g.text(font, Component.translatable("screen.bcenchantments.forge.order"), x0, y + 4, Theme.argb(alpha, Theme.TEXT_MUTED), false);
		y += 24;
		if (chosen.isEmpty()) {
			g.text(font, Component.translatable("screen.bcenchantments.forge.none"), x0, y, Theme.argb(alpha, Theme.TEXT_FAINT), false);
			return;
		}
		for (int i = 0; i < chosen.size(); i += 2) {
			Refinement.Perk first = chosen.get(i).perk();
			Refinement.Perk second = i + 1 < chosen.size() ? chosen.get(i + 1).perk() : null;
			Component names = first.displayName();
			Component result;
			int color = Theme.TEXT_FAINT;
			if (second == null) {
				result = Component.translatable("screen.bcenchantments.forge.alone");
			} else {
				names = Component.empty().append(first.displayName()).append(" + ").append(second.displayName());
				Synergy synergy = Synergy.of(first.enchantment(), second.enchantment());
				result = synergy == null ? Component.translatable("screen.bcenchantments.forge.no_combination") : synergy.displayName();
				color = synergy == null ? Theme.TEXT_FAINT : Theme.TEXT_BRIGHT;
			}
			g.text(font, names, x0, y, Theme.argb(alpha, Theme.TEXT_BODY), false);
			g.text(font, Component.literal("> ").append(result), x0, y + 10, Theme.argb(alpha, color), false);
			y += 26;
		}
	}

	private void chip(GuiGraphicsExtractor g, Refinement.Perk perk, int level, boolean locked, int x0, int y0, int x1, int y1,
		boolean hovered, float alpha) {
		boolean selected = level > 0;
		Theme.box(g, x0, y0, x1, y1, selected ? Theme.argb(alpha, Theme.ACCENT) : Theme.argb(alpha * Theme.PANEL_ALPHA, Theme.SUNKEN));
		int ring = selected ? Theme.TEXT_MUTED : hovered && !locked ? Theme.BORDER_HOVER : Theme.BORDER_CONTROL;
		Theme.hairline(g, x0, y0, x1, y1, Theme.argb(alpha, locked ? Theme.BORDER : ring));
		int color = locked ? Theme.TEXT_FAINT : selected ? Theme.ON_ACCENT : hovered ? Theme.TEXT_BODY : Theme.TEXT_MUTED;
		g.text(font, perk.displayName(), x0 + 4, y0 + 2, Theme.argb(alpha, color), false);
		if (selected) {
			String numeral = NUMERALS[level];
			g.text(font, numeral, x1 - 4 - font.width(numeral), y0 + 2, Theme.argb(alpha, Theme.ON_ACCENT), false);
		}
	}

	private void button(GuiGraphicsExtractor g, String text, int x0, int y0, int x1, int y1, int mouseX, int mouseY, float alpha,
		IntConsumer action, @Nullable Component reason) {
		boolean hovered = mouseX >= x0 && mouseX < x1 && mouseY >= y0 && mouseY < y1;
		Theme.box(g, x0, y0, x1, y1, Theme.argb(alpha * Theme.PANEL_ALPHA, Theme.SUNKEN));
		Theme.hairline(g, x0, y0, x1, y1, Theme.argb(alpha, hovered ? Theme.BORDER_HOVER : Theme.BORDER_CONTROL));
		g.text(font, text, (x0 + x1 - font.width(text) + 1) / 2, (y0 + y1) / 2 - 4, Theme.argb(alpha, hovered ? Theme.TEXT_BRIGHT : Theme.TEXT_BODY), false);
		hits.add(new Hit(x0, y0, x1, y1, action, reason));
	}

	/** The one filled slab on the screen: forging is what it exists for. */
	private void forgeButton(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int mouseX, int mouseY, float alpha) {
		boolean hovered = mouseX >= x0 && mouseX < x1 && mouseY >= y0 && mouseY < y1;
		Theme.box(g, x0, y0, x1, y1, Theme.argb(alpha, Theme.ACCENT));
		Theme.hairline(g, x0, y0, x1, y1, Theme.argb(alpha, hovered ? Theme.TEXT_BRIGHT : Theme.TEXT_MUTED));
		String text = Component.translatable("screen.bcenchantments.forge.forge").getString();
		g.text(font, text, (x0 + x1 - font.width(text)) / 2, (y0 + y1) / 2 - 4, Theme.argb(alpha, Theme.ON_ACCENT), false);
		hits.add(new Hit(x0, y0, x1, y1, click -> forge(), null));
	}

	private int indexOf(Refinement.Perk perk) {
		for (int i = 0; i < chosen.size(); i++) {
			if (chosen.get(i).perk() == perk) {
				return i;
			}
		}
		return -1;
	}

	/** Moves to the next or previous weapon, dropping the powers it cannot take. */
	private void pick(int step) {
		if (weapons.isEmpty()) {
			return;
		}
		weapon = Math.floorMod(weapon + step, weapons.size());
		ItemStack base = base();
		List<Refinement.Power> kept = new ArrayList<>();
		for (Refinement.Power power : chosen) {
			Refinement.Perk perk = power.perk();
			if (perk != null && Refinement.lockedReason(perk, base, List.of()) == null && (kept.isEmpty() || !Refinement.isShield(base))) {
				kept.add(Refinement.isShield(base) ? new Refinement.Power(power.trim(), 1) : power);
			}
		}
		chosen.clear();
		chosen.addAll(kept);
		status = Component.empty();
	}

	/** Left click raises a power a level and, past the last, takes it off; right click takes it off. */
	private void click(Refinement.Perk perk, int button) {
		ItemStack base = base();
		int index = indexOf(perk);
		int top = Refinement.isShield(base) ? 1 : Refinement.MAX_PERK;
		if (index < 0) {
			if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && Refinement.lockedReason(perk, base, chosen) == null) {
				chosen.add(new Refinement.Power(Refinement.trim(perk), 1));
			}
		} else if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && chosen.get(index).level() < top) {
			chosen.set(index, new Refinement.Power(chosen.get(index).trim(), chosen.get(index).level() + 1));
		} else {
			chosen.remove(index);
		}
		status = Component.empty();
	}

	private void forge() {
		if (weapons.isEmpty()) {
			return;
		}
		if (minecraft.player == null || !ClientPlayNetworking.canSend(ForgePayload.TYPE)) {
			status = Component.translatable("screen.bcenchantments.forge.no_world");
			return;
		}
		ClientPlayNetworking.send(new ForgePayload(BuiltInRegistries.ITEM.getKey(weapons.get(weapon)), List.copyOf(chosen)));
		status = Component.translatable("screen.bcenchantments.forge.sent");
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		for (Hit hit : hits) {
			if (hit.contains(event.x(), event.y())) {
				hit.action().accept(event.button());
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public void onClose() {
		minecraft.gui.setScreen(parent);
	}
}
