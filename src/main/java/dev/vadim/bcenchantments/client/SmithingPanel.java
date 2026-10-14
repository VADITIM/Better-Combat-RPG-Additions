package dev.vadim.bcenchantments.client;

import dev.vadim.bcenchantments.combat.CriticalHits;
import dev.vadim.bcenchantments.network.SpendPointPayload;
import dev.vadim.bcenchantments.perk.Synergy;
import dev.vadim.bcenchantments.progression.Refinement;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.SmithingScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * The window to the left of the smithing table: the weapon or shield in the middle slot, its stats,
 * refinement and trim powers, the pairs they make, what the inputs on the table would do right now,
 * what the next step costs and which trims could combine with it. It scrolls with the mouse wheel
 * when it runs past its height, half again the table's. A toggle beside the table opens and closes it
 * like the recipe book: open, it pushes the table right and the two are centred together. Drawn and
 * laid out by {@code SmithingScreenMixin}.
 */
public final class SmithingPanel {
	private static final int SMITHING_WIDTH = 176;
	private static final int SMITHING_HEIGHT = 166;
	private static final int SPACING = 4;
	private static final int MIN_WIDTH = 120;
	private static final int MAX_WIDTH = 168;
	private static final int PADDING = 6;
	private static final int LINE = 10;
	private static final int SLOT = 18;
	private static final int SECTION_GAP = 6;
	private static final int SCROLL_STEP = 20;
	/** Space between the trims in the "possible" list. */
	private static final int TRIM_GAP = 8;
	private static final DecimalFormat NUMBER = new DecimalFormat("0.##");
	/** The player's own attack damage, attack speed and critical damage, which a weapon adds to. */
	private static final double BASE_DAMAGE = 1.0;
	private static final double BASE_SPEED = 4.0;
	private static final double BASE_CRIT_DAMAGE = 1.5;

	/** Width of the toggle and "?" buttons beside the table, with their gap. */
	private static final int BUTTON_COLUMN = 24;

	/** Whether the panel is shown; it stays as the player left it, like the recipe book. */
	private static boolean open = true;
	private static long openedAt;
	private static long changedAt;
	private static float scroll;
	private static int maxScroll;
	private static int @Nullable [] bounds;
	private static ItemStack shown = ItemStack.EMPTY;
	/** The trim whose combinations are listed in full, if one was clicked. */
	private static Refinement.@Nullable Perk expanded;
	/** Where the clickable trims and level points were drawn this frame, inside the visible part of the panel. */
	private static final List<Hit> hits = new ArrayList<>();
	private static int @Nullable [] view;

	/** What one frame draws with; rows read it instead of taking a long parameter list. */
	private static GuiGraphicsExtractor g;
	private static Font font;
	private static float alpha;
	private static int mouseX;
	private static int mouseY;
	private static ItemStack hovered = ItemStack.EMPTY;

	private SmithingPanel() {
	}

	/** One piece of the panel, laid out top to bottom. */
	private interface Row {
		int height(int width);

		void draw(int x, int y, int width);
	}

	/** The item, its name and a line under it. */
	private record Header(ItemStack stack, Component name, Component sub) implements Row {
		@Override
		public int height(int width) {
			return SLOT + 2;
		}

		@Override
		public void draw(int x, int y, int width) {
			slot(stack, x, y);
			int tx = x + SLOT + 4;
			g.text(font, ellipsize(name, width - SLOT - 4), tx, y, Theme.argb(alpha, Theme.TEXT_BRIGHT), true);
			g.text(font, sub, tx, y + LINE, Theme.argb(alpha, Theme.TEXT_LABEL), false);
		}
	}

	/** A section heading over a hairline. */
	private record Heading(Component text) implements Row {
		@Override
		public int height(int width) {
			return SECTION_GAP + LINE + 2;
		}

		@Override
		public void draw(int x, int y, int width) {
			g.fill(x, y + SECTION_GAP / 2 - 1, x + width, y + SECTION_GAP / 2, Theme.argb(alpha, Theme.BORDER));
			g.text(font, text, x, y + SECTION_GAP, Theme.argb(alpha, Theme.TEXT_MUTED), false);
		}
	}

	/** A wrapped paragraph. */
	private record Text(Component text, int color, int indent) implements Row {
		Text(Component text, int color) {
			this(text, color, 0);
		}

		@Override
		public int height(int width) {
			return font.split(text, width - indent).size() * LINE;
		}

		@Override
		public void draw(int x, int y, int width) {
			lines(text, x + indent, y, width - indent, color);
		}
	}

	/** A stat: its name on the left, its value on the right, and the value the table's inputs would give. */
	private record Stat(Component label, String value, @Nullable String next) implements Row {
		@Override
		public int height(int width) {
			return LINE;
		}

		@Override
		public void draw(int x, int y, int width) {
			g.text(font, label, x, y, Theme.argb(alpha, Theme.TEXT_LABEL), false);
			int right = x + width;
			if (next != null) {
				right -= font.width(next);
				g.text(font, next, right, y, Theme.argb(alpha, Theme.TEXT_BRIGHT), false);
				right -= font.width(" → ");
				g.text(font, " → ", right, y, Theme.argb(alpha, Theme.TEXT_FAINT), false);
			}
			g.text(font, value, right - font.width(value), y, Theme.argb(alpha, next != null ? Theme.TEXT_MUTED : Theme.TEXT_BODY), false);
		}
	}

	/** Emeralds paid toward the next level as charge blocks: paid, about to be paid, still needed. */
	private record Bar(int paid, int incoming, int total) implements Row {
		@Override
		public int height(int width) {
			return 6;
		}

		@Override
		public void draw(int x, int y, int width) {
			g.fill(x, y + 1, x + width, y + 5, Theme.argb(alpha * 0.25F, Theme.ACCENT));
			int filled = total <= 0 ? 0 : Math.round(width * Mth.clamp(paid / (float) total, 0.0F, 1.0F));
			int coming = total <= 0 ? 0 : Math.round(width * Mth.clamp((paid + incoming) / (float) total, 0.0F, 1.0F));
			g.fill(x + filled, y + 1, x + coming, y + 5, Theme.argb(alpha, Theme.TEXT_FAINT));
			g.fill(x, y + 1, x + filled, y + 5, Theme.argb(alpha, Theme.TEXT_BODY));
		}
	}

	/** An item icon with a title beside it and optional wrapped text under the title. */
	private record Icon(ItemStack stack, Component title, int color, @Nullable Component sub, int subColor) implements Row {
		@Override
		public int height(int width) {
			int text = LINE + (sub == null ? 0 : font.split(sub, width - SLOT - 4).size() * LINE);
			return Math.max(SLOT, text) + 1;
		}

		@Override
		public void draw(int x, int y, int width) {
			slot(stack, x, y);
			int tx = x + SLOT + 4;
			g.text(font, ellipsize(title, width - SLOT - 4), tx, y + (sub == null ? 5 : 1), Theme.argb(alpha, color), false);
			if (sub != null) {
				lines(sub, tx, y + 1 + LINE, width - SLOT - 4, subColor);
			}
		}
	}

	/** A trim the player carries; a click opens or closes the list of everything it combines into. */
	private record TrimRow(Refinement.Perk perk, Icon icon) implements Row {
		@Override
		public int height(int width) {
			return icon.height(width);
		}

		@Override
		public void draw(int x, int y, int width) {
			int h = icon.height(width);
			boolean over = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + h;
			if (over || expanded == perk) {
				g.fill(x - 2, y - 1, x + width + 2, y + h, Theme.argb(alpha * (over ? 0.12F : 0.07F), 0xFFFFFF));
			}
			icon.draw(x, y, width);
			String mark = expanded == perk ? "-" : "+";
			g.text(font, mark, x + width - font.width(mark), y + 1, Theme.argb(alpha, over ? Theme.TEXT_BRIGHT : Theme.TEXT_FAINT), false);
			hits.add(new Hit(x - 2, y - 1, x + width + 2, y + h, () -> expanded = expanded == perk ? null : perk));
		}
	}

	/**
	 * A stat a level point can go to: its name, the points already on it, and a "+" while the weapon has
	 * one to spend. A click spends it; the server changes the weapon on the table.
	 */
	private record PointRow(Refinement.Stat stat, int spent, boolean open) implements Row {
		@Override
		public int height(int width) {
			return LINE + 2;
		}

		@Override
		public void draw(int x, int y, int width) {
			int h = height(width);
			boolean over = open && mouseX >= x && mouseX < x + width && mouseY >= y - 1 && mouseY < y + h - 1;
			if (over) {
				g.fill(x - 2, y - 1, x + width + 2, y + h - 1, Theme.argb(alpha * 0.12F, 0xFFFFFF));
			}
			g.text(font, text("stat." + stat.id()), x, y + 1, Theme.argb(alpha, open ? Theme.TEXT_BODY : Theme.TEXT_LABEL), false);
			String value = spent > 0 ? "+" + NUMBER.format(spent * Refinement.POINT_BOOST * 100.0) + "%" : "";
			String mark = open ? "+" : "";
			int right = x + width;
			if (!mark.isEmpty()) {
				right -= font.width(mark);
				g.text(font, mark, right, y + 1, Theme.argb(alpha, over ? Theme.TEXT_BRIGHT : Theme.TEXT_MUTED), false);
				right -= 6;
			}
			g.text(font, value, right - font.width(value), y + 1, Theme.argb(alpha, Theme.TEXT_MUTED), false);
			if (open) {
				hits.add(new Hit(x - 2, y - 1, x + width + 2, y + h - 1, () -> ClientPlayNetworking.send(new SpendPointPayload(stat.id()))));
			}
		}
	}

	private record Hit(int x0, int y0, int x1, int y1, Runnable action) {
	}

	private record Space(int size) implements Row {
		@Override
		public int height(int width) {
			return size;
		}

		@Override
		public void draw(int x, int y, int width) {
		}
	}

	/** Moves the scroll wheel to the panel while the mouse is over it, and starts each table fresh. */
	static void register() {
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (!(screen instanceof SmithingScreen)) {
				return;
			}
			openedAt = Util.getMillis();
			changedAt = openedAt;
			scroll = 0.0F;
			shown = ItemStack.EMPTY;
			bounds = null;
			expanded = null;
			hits.clear();
			ScreenMouseEvents.allowMouseScroll(screen).register((s, x, y, horizontal, vertical) -> {
				if (!contains(x, y)) {
					return true;
				}
				scroll = Mth.clamp(scroll - (float) vertical * SCROLL_STEP, 0.0F, maxScroll);
				return false;
			});
			ScreenMouseEvents.allowMouseClick(screen).register((s, event) -> {
				int[] v = view;
				if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT || v == null || event.x() < v[0] || event.x() >= v[2] || event.y() < v[1] || event.y() >= v[3]) {
					return true;
				}
				for (Hit hit : hits) {
					if (event.x() >= hit.x0() && event.x() < hit.x1() && event.y() >= hit.y0() && event.y() < hit.y1()) {
						hit.action().run();
						Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
						return false;
					}
				}
				return true;
			});
		});
	}

	/** Whether ({@code x}, {@code y}) is over the panel, so a click there does not throw the carried item. */
	public static boolean contains(double x, double y) {
		int[] b = bounds;
		return b != null && x >= b[0] && x < b[2] && y >= b[1] && y < b[3];
	}

	public static boolean isOpen() {
		return open;
	}

	/** Opens or closes the panel; the table moves to make room, like the recipe book. */
	public static void toggle() {
		open = !open;
		openedAt = Util.getMillis();
		changedAt = openedAt;
	}

	/** How wide the panel is on a screen this wide: 0 when closed or when it does not fit beside the table. */
	public static int width(int screenWidth) {
		if (!open) {
			return 0;
		}
		// Room for the table, the gap, and the button column on the table's right, kept clear of both edges.
		int w = Math.min(MAX_WIDTH, screenWidth - SMITHING_WIDTH - SPACING - 2 * (BUTTON_COLUMN + SPACING));
		return w < MIN_WIDTH ? 0 : w;
	}

	/** The table's left edge with the panel open: panel and table centred together, like the recipe book. */
	public static int tableLeft(int screenWidth) {
		int w = width(screenWidth);
		return w == 0 ? (screenWidth - SMITHING_WIDTH) / 2 : (screenWidth - w - SPACING - SMITHING_WIDTH) / 2 + w + SPACING;
	}

	public static void extract(SmithingScreen screen, GuiGraphicsExtractor graphics, int mx, int my, int left, int top) {
		Player player = Minecraft.getInstance().player;
		int w = width(screen.width);
		int x1 = left - SPACING;
		int x0 = x1 - w;
		hits.clear();
		if (player == null || w == 0) {
			bounds = null;
			view = null;
			return;
		}
		// Half again the table's height, reaching a quarter of it past the table above and below.
		int y0 = Math.max(SPACING, top - SMITHING_HEIGHT / 4);
		int y1 = Math.min(screen.height - SPACING, top + SMITHING_HEIGHT + SMITHING_HEIGHT / 4);
		bounds = new int[] {x0, y0, x1, y1};

		ItemStack template = screen.getMenu().getSlot(0).getItem();
		ItemStack base = screen.getMenu().getSlot(1).getItem();
		ItemStack addition = screen.getMenu().getSlot(2).getItem();
		long now = Util.getMillis();
		if (!ItemStack.isSameItemSameComponents(shown, base)) {
			if (shown.getItem() != base.getItem()) {
				scroll = 0.0F;
			}
			shown = base.copy();
			changedAt = now;
		}

		g = graphics;
		font = Minecraft.getInstance().font;
		mouseX = mx;
		mouseY = my;
		hovered = ItemStack.EMPTY;
		float panelAlpha = Theme.easeOut((now - openedAt) / (float) Theme.ENTER_MS);
		Theme.panel(g, x0, y0, x1, y1, panelAlpha, Theme.BORDER);

		int viewX0 = x0 + PADDING;
		int viewX1 = x1 - PADDING;
		int viewY0 = y0 + PADDING;
		int viewY1 = y1 - PADDING;
		int width = viewX1 - viewX0 - 4;
		view = new int[] {viewX0, viewY0, viewX1, viewY1};
		List<Row> rows = rows(player, template, base, addition);
		int total = 0;
		for (Row row : rows) {
			total += row.height(width);
		}
		maxScroll = Math.max(0, total - (viewY1 - viewY0));
		scroll = Mth.clamp(scroll, 0.0F, maxScroll);

		float settle = Theme.easeOut((now - changedAt) / 250.0F);
		alpha = panelAlpha * settle;
		g.enableScissor(viewX0, viewY0, viewX1, viewY1);
		g.pose().pushMatrix();
		g.pose().translate(0.0F, (1.0F - settle) * 6.0F);
		int y = viewY0 - Math.round(scroll);
		for (Row row : rows) {
			int h = row.height(width);
			if (y + h >= viewY0 && y <= viewY1) {
				row.draw(viewX0, y, width);
			}
			y += h;
		}
		g.pose().popMatrix();
		g.disableScissor();

		if (maxScroll > 0) {
			int track = viewY1 - viewY0;
			int thumb = Math.max(12, track * track / (track + maxScroll));
			int ty = viewY0 + Math.round((track - thumb) * scroll / maxScroll);
			g.fill(viewX1 - 2, viewY0, viewX1, viewY1, Theme.argb(panelAlpha * 0.5F, Theme.BORDER));
			g.fill(viewX1 - 2, ty, viewX1, ty + thumb, Theme.argb(panelAlpha, Theme.BORDER_HOVER));
		}
		if (!hovered.isEmpty() && mx >= viewX0 && mx < viewX1 && my >= viewY0 && my < viewY1) {
			g.setTooltipForNextFrame(font, hovered, mx, my);
		}
		g = null;
	}

	// ---- Content ----

	private static List<Row> rows(Player player, ItemStack template, ItemStack base, ItemStack addition) {
		List<Row> rows = new ArrayList<>();
		if (base.isEmpty()) {
			rows.add(new Text(text("empty"), Theme.TEXT_BODY));
			rows.add(new Space(6));
			rows.add(new Text(text("empty.steps"), Theme.TEXT_MUTED));
			return rows;
		}
		if (Refinement.isShield(base)) {
			shield(rows, player, template, base, addition);
		} else if (Refinement.isWeapon(base)) {
			weapon(rows, player, template, base, addition);
		} else {
			rows.add(new Header(base, base.getHoverName(), Component.empty()));
			rows.add(new Text(text("other"), Theme.TEXT_MUTED));
		}
		return rows;
	}

	private static void weapon(List<Row> rows, Player player, ItemStack template, ItemStack base, ItemStack addition) {
		int seed = player.getEnchantmentSeed();
		Refinement refinement = Refinement.current(base, seed);
		Refinement.Plan plan = Refinement.plan(template, base, addition, seed);
		ItemStack result = plan == null ? ItemStack.EMPTY : plan.result();
		rows.add(new Header(base, base.getHoverName(), levelName(refinement.level())));

		preview(rows, player, template, base, addition, refinement, plan);

		rows.add(new Heading(text("section.stats")));
		stat(rows, "damage", base, result, stack -> BASE_DAMAGE + sum(stack, Attributes.ATTACK_DAMAGE), "");
		stat(rows, "speed", base, result, stack -> BASE_SPEED + sum(stack, Attributes.ATTACK_SPEED), "");
		stat(rows, "crit_chance", base, result, stack -> sum(stack, CriticalHits.CRITICAL_CHANCE) * 100.0, "%");
		stat(rows, "crit_damage", base, result, stack -> BASE_CRIT_DAMAGE + sum(stack, CriticalHits.CRITICAL_DAMAGE), "×");
		points(rows, base);

		if (refinement.level() < Refinement.MAX_LEVEL) {
			refining(rows, player, refinement, plan);
		} else {
			powers(rows, player, base);
			pairs(rows, base);
			next(rows, player, base);
		}
	}

	/** What the inputs on the table do right now, or why they do nothing. */
	private static void preview(List<Row> rows, Player player, ItemStack template, ItemStack base, ItemStack addition,
		@Nullable Refinement refinement, Refinement.@Nullable Plan plan) {
		if (template.isEmpty() && addition.isEmpty()) {
			return;
		}
		List<Component> lines = new ArrayList<>();
		boolean blocked = false;
		if (plan != null) {
			Refinement after = plan.result().get(Refinement.COMPONENT);
			String trim = Refinement.trimOf(template);
			if (trim == null && after != null) {
				int paid = plan.additionItems() * Refinement.gemValue(addition);
				lines.add(text("preview.pay", paid));
				lines.add(plan.levelUp() ? text("preview.level", numeral(after.level())) : text("preview.progress", after.gems(), after.cost()));
			} else if (trim != null) {
				Refinement.Perk perk = Refinement.perk(trim);
				int owned = Refinement.perkLevel(base, perk.enchantment());
				if (owned > 0) {
					lines.add(text("preview.raise", perk.displayName(), numeral(owned + 1)));
				} else {
					lines.add(text("preview.add", perk.displayName()));
					Refinement.Perk waiting = Refinement.unpaired(base);
					if (Refinement.isShield(base)) {
						// A shield's one power pairs with nothing.
					} else if (waiting == null) {
						lines.add(text("preview.new_pair"));
					} else {
						Synergy synergy = Synergy.of(waiting.enchantment(), perk.enchantment());
						lines.add(synergy == null ? text("preview.no_combination", waiting.displayName())
							: text("preview.combines", waiting.displayName(), synergy.displayName()));
					}
					String locked = Refinement.lockedReason(perk, base, Refinement.powersOf(base));
					if (locked != null) {
						lines.add(Component.translatable(locked));
						blocked = true;
					}
				}
				if (plan.levelCost() > 0) {
					boolean affordable = player.hasInfiniteMaterials() || player.experienceLevel >= plan.levelCost();
					lines.add(text("preview.cost", plan.levelCost()));
					blocked |= !affordable;
				} else {
					lines.add(text("preview.free"));
				}
			}
		} else {
			Component why = why(template, base, addition, refinement);
			if (why == null) {
				return;
			}
			lines.add(why);
			blocked = true;
		}
		rows.add(new Heading(text("section.preview")));
		for (Component line : lines) {
			rows.add(new Text(line, blocked ? Theme.DANGER : Theme.TEXT_BRIGHT));
		}
	}

	/** Why the table's inputs make nothing, or null if there is nothing useful to say. */
	private static @Nullable Component why(ItemStack template, ItemStack base, ItemStack addition, @Nullable Refinement refinement) {
		String trim = Refinement.trimOf(template);
		if (Refinement.isShield(base)) {
			if (trim == null) {
				return null;
			}
			Refinement.Perk perk = Refinement.perk(trim);
			if (!Refinement.onShield(perk)) {
				return Component.translatable("forge.bcenchantments.locked.shield");
			}
			if (!Refinement.powersOf(base).isEmpty()) {
				return Component.translatable("forge.bcenchantments.locked.shield_one");
			}
			return addition.is(Items.ECHO_SHARD) ? null : text("why.shard");
		}
		if (refinement == null) {
			return null;
		}
		if (template.isEmpty() && Refinement.gemValue(addition) > 0) {
			return refinement.level() >= Refinement.MAX_LEVEL ? text("why.refined") : null;
		}
		if (trim == null) {
			return template.isEmpty() && !addition.isEmpty() && !addition.is(Items.ECHO_SHARD) ? text("why.gems") : null;
		}
		if (refinement.level() < Refinement.MAX_LEVEL) {
			return text("why.level");
		}
		Refinement.Perk perk = Refinement.perk(trim);
		if (!perk.built()) {
			return text("why.unbuilt");
		}
		int owned = Refinement.perkLevel(base, perk.enchantment());
		if (owned >= Refinement.MAX_PERK) {
			return text("why.max", perk.displayName());
		}
		if (owned > 0) {
			return addition.isEmpty() ? null : text("why.no_shard");
		}
		return addition.is(Items.ECHO_SHARD) ? null : text("why.shard");
	}

	private interface StatOf {
		double of(ItemStack stack);
	}

	private static void stat(List<Row> rows, String name, ItemStack base, ItemStack result, StatOf stat, String unit) {
		double now = stat.of(base);
		String next = null;
		if (!result.isEmpty()) {
			double then = stat.of(result);
			if (Math.abs(then - now) > 1.0E-4) {
				next = format(then, unit);
			}
		}
		rows.add(new Stat(text("stat." + name), format(now, unit), next));
	}

	/** The weapon's level points: one per refinement level, each spent on the stat the player clicks. */
	private static void points(List<Row> rows, ItemStack base) {
		Refinement refinement = base.get(Refinement.COMPONENT);
		if (refinement == null || refinement.level() == 0) {
			return;
		}
		int left = refinement.unspentPoints();
		rows.add(new Heading(text("section.points")));
		rows.add(new Text(left > 0 ? text("points.left", left) : text("points.none"), left > 0 ? Theme.TEXT_BRIGHT : Theme.TEXT_MUTED));
		rows.add(new Space(2));
		for (Refinement.Stat stat : Refinement.Stat.values()) {
			rows.add(new PointRow(stat, refinement.points().of(stat), left > 0));
		}
		rows.add(new Text(text(left > 0 ? "points.how" : "points.earn"), Theme.TEXT_FAINT));
	}

	private static String format(double value, String unit) {
		return unit.equals("×") ? "×" + NUMBER.format(value) : NUMBER.format(value) + unit;
	}

	/** Sum of the stack's main-hand additions to {@code attribute}. */
	private static double sum(ItemStack stack, Holder<Attribute> attribute) {
		double[] sum = {0.0};
		stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).forEach(EquipmentSlotGroup.MAINHAND, (holder, modifier) -> {
			if (holder.equals(attribute) && modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
				sum[0] += modifier.amount();
			}
		});
		return sum[0];
	}

	private static void refining(List<Row> rows, Player player, Refinement refinement, Refinement.@Nullable Plan plan) {
		rows.add(new Heading(text("section.refinement")));
		int level = refinement.level();
		rows.add(new Text(text("level_of", numeral(level + 1), numeral(Refinement.MAX_LEVEL)), Theme.TEXT_BODY));
		int incoming = 0;
		if (plan != null && plan.result().get(Refinement.COMPONENT) instanceof Refinement after) {
			incoming = after.level() > level ? refinement.cost() - refinement.gems() : after.gems() - refinement.gems();
		}
		rows.add(new Space(2));
		rows.add(new Bar(refinement.gems(), incoming, refinement.cost()));
		rows.add(new Text(text("gems", refinement.gems(), refinement.cost()), Theme.TEXT_MUTED));
		rows.add(new Space(4));
		rows.add(new Text(text("next_level"), Theme.TEXT_BODY));
		if (level + 1 == Refinement.MAX_LEVEL) {
			rows.add(new Text(text("unlocks"), Theme.TEXT_BRIGHT));
		}

		rows.add(new Heading(text("section.next")));
		int needed = refinement.cost() - refinement.gems();
		int carried = emeralds(player);
		rows.add(new Text(text("needs", needed), Theme.TEXT_BODY));
		boolean enough = player.hasInfiniteMaterials() || carried >= needed;
		rows.add(new Text(text("carry", carried), enough ? Theme.TEXT_MUTED : Theme.DANGER));
		rows.add(new Space(2));
		rows.add(new Text(text("how_refine"), Theme.TEXT_FAINT));
	}

	/** Emeralds in the inventory, a block counting as nine. */
	private static int emeralds(Player player) {
		int total = 0;
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack stack = player.getInventory().getItem(i);
			total += Refinement.gemValue(stack) * stack.getCount();
		}
		return total;
	}

	private static void powers(List<Row> rows, Player player, ItemStack base) {
		rows.add(new Heading(text("section.powers")));
		List<Refinement.Power> powers = Refinement.powersOf(base);
		if (powers.isEmpty()) {
			rows.add(new Text(text("no_powers"), Theme.TEXT_BODY));
			return;
		}
		for (Refinement.Power power : powers) {
			Refinement.Perk perk = power.perk();
			if (perk == null) {
				continue;
			}
			Component name = Component.empty().append(perk.displayName()).append(" ").append(numeral(power.level()));
			rows.add(new Icon(template(power.trim()), name, Theme.perkColor(perk.id()), description("tooltip.bcenchantments.power." + perk.id()),
				Theme.TEXT_MUTED));
			if (power.level() < Refinement.MAX_PERK) {
				boolean has = carries(player, power.trim());
				rows.add(new Text(text("raise", Refinement.trimName(power.trim())), has ? Theme.TEXT_BODY : Theme.TEXT_FAINT, SLOT + 4));
			} else {
				rows.add(new Text(text("max"), Theme.TEXT_FAINT, SLOT + 4));
			}
			rows.add(new Space(3));
		}
	}

	/** The pairs the powers make, in the order they were applied, and the refined combinations that are on. */
	private static void pairs(List<Row> rows, ItemStack base) {
		List<Refinement.Power> powers = Refinement.powersOf(base);
		boolean refinedOn = false;
		for (Synergy synergy : Synergy.values()) {
			refinedOn |= synergy.perks().size() != 2 && synergy.on(base);
		}
		if (powers.size() < 2 && !refinedOn) {
			return;
		}
		rows.add(new Heading(text("section.pairs")));
		for (int i = 0; i + 1 < powers.size(); i += 2) {
			Refinement.Perk first = powers.get(i).perk();
			Refinement.Perk second = powers.get(i + 1).perk();
			if (first == null || second == null) {
				continue;
			}
			Synergy synergy = Synergy.of(first.enchantment(), second.enchantment());
			rows.add(new Text(text("pair", first.displayName(), second.displayName()), Theme.TEXT_MUTED));
			if (synergy == null) {
				rows.add(new Text(text("no_combination"), Theme.TEXT_FAINT, 6));
			} else {
				rows.add(new Text(Component.literal("→ ").append(synergy.displayName()), Theme.TEXT_BRIGHT, 6));
				rows.add(new Text(description("tooltip.bcenchantments.synergy." + synergy.id()), Theme.TEXT_MUTED, 6));
			}
			rows.add(new Space(3));
		}
		if (powers.size() % 2 == 1 && powers.getLast().perk() != null) {
			rows.add(new Text(text("alone", powers.getLast().perk().displayName()), Theme.TEXT_FAINT));
		}
		for (Synergy synergy : Synergy.values()) {
			if (synergy.perks().size() != 2 && synergy.on(base)) {
				rows.add(new Text(synergy.displayName(), Theme.TEXT_BRIGHT));
				rows.add(new Text(description("tooltip.bcenchantments.synergy." + synergy.id()), Theme.TEXT_MUTED, 6));
			}
		}
	}

	/** What the next power costs and which trims would make a combination with what is on the weapon. */
	private static void next(List<Row> rows, Player player, ItemStack base) {
		List<Refinement.Power> powers = Refinement.powersOf(base);
		rows.add(new Heading(text("section.next")));
		int cost = Refinement.powerCost(powers.size());
		if (cost == 0) {
			rows.add(new Text(text("first_free"), Theme.TEXT_BODY));
		} else {
			boolean affordable = player.hasInfiniteMaterials() || player.experienceLevel >= cost;
			rows.add(new Text(text("power_cost", cost), affordable ? Theme.TEXT_BODY : Theme.DANGER));
		}
		rows.add(new Text(text("how_trim"), Theme.TEXT_FAINT));

		rows.add(new Heading(text("section.possible")));
		Refinement.Perk waiting = Refinement.unpaired(base);
		if (waiting != null) {
			rows.add(new Text(text("waiting", waiting.displayName()), Theme.TEXT_BODY));
			rows.add(new Space(2));
			boolean any = false;
			for (Refinement.Perk partner : candidates(base)) {
				Synergy synergy = Synergy.of(waiting.enchantment(), partner.enchantment());
				if (synergy == null) {
					continue;
				}
				any = true;
				String trim = Refinement.trim(partner);
				boolean has = carries(player, trim);
				rows.add(new Icon(template(trim), synergy.displayName(), has ? Theme.TEXT_BRIGHT : Theme.TEXT_MUTED,
					text("from", Refinement.trimName(trim), partner.displayName()), has ? Theme.TEXT_MUTED : Theme.TEXT_FAINT));
			}
			if (!any) {
				rows.add(new Text(text("no_partner"), Theme.TEXT_FAINT));
			}
			return;
		}
		rows.add(new Text(text(powers.isEmpty() ? "starts" : "new_pair"), Theme.TEXT_BODY));
		rows.add(new Space(2));
		boolean any = false;
		List<Refinement.Perk> candidates = candidates(base);
		for (Refinement.Perk perk : candidates) {
			String trim = Refinement.trim(perk);
			if (!carries(player, trim)) {
				continue;
			}
			any = true;
			// What the power does on its own; a click lists what it combines into.
			Component sub = description("tooltip.bcenchantments.power." + perk.id());
			rows.add(new TrimRow(perk, new Icon(template(trim), perk.displayName(), Theme.perkColor(perk.id()), sub, Theme.TEXT_MUTED)));
			if (expanded == perk) {
				combinations(rows, perk, candidates);
			}
			rows.add(new Space(TRIM_GAP));
		}
		if (any) {
			rows.add(new Text(text("click_hint"), Theme.TEXT_FAINT));
		} else {
			rows.add(new Text(text("none_carried"), Theme.TEXT_FAINT));
		}
	}

	/**
	 * Every combination {@code perk} is part of, not only the ones this weapon could still make: each with
	 * its partner, the partner's trim and what it does. Partners this weapon can no longer take are faint.
	 */
	private static void combinations(List<Row> rows, Refinement.Perk perk, List<Refinement.Perk> candidates) {
		int indent = SLOT + 4;
		rows.add(new Space(3));
		boolean any = false;
		for (Refinement.Perk partner : Refinement.Perk.values()) {
			Synergy synergy = partner == perk || !partner.built() ? null : Synergy.of(perk.enchantment(), partner.enchantment());
			if (synergy == null) {
				continue;
			}
			any = true;
			boolean open = candidates.contains(partner);
			rows.add(new Text(synergy.displayName(), open ? Theme.TEXT_BRIGHT : Theme.TEXT_MUTED, indent));
			rows.add(new Text(text("combo_with", partner.displayName(), Refinement.trimName(Refinement.trim(partner))),
				open ? Theme.TEXT_BODY : Theme.TEXT_FAINT, indent));
			rows.add(new Text(description("tooltip.bcenchantments.synergy." + synergy.id()), open ? Theme.TEXT_MUTED : Theme.TEXT_FAINT, indent));
			rows.add(new Space(4));
		}
		if (!any) {
			rows.add(new Text(text("no_combinations"), Theme.TEXT_FAINT, indent));
		}
	}

	/** The powers {@code base} could still take: built, not on it yet, and not refused. */
	private static List<Refinement.Perk> candidates(ItemStack base) {
		List<Refinement.Power> powers = Refinement.powersOf(base);
		List<Refinement.Perk> perks = new ArrayList<>();
		for (Refinement.Perk perk : Refinement.Perk.values()) {
			if (perk.built() && Refinement.perkLevel(base, perk.enchantment()) == 0 && Refinement.lockedReason(perk, base, powers) == null) {
				perks.add(perk);
			}
		}
		return perks;
	}

	private static void shield(List<Row> rows, Player player, ItemStack template, ItemStack base, ItemStack addition) {
		rows.add(new Header(base, base.getHoverName(), text("shield")));
		Refinement.Plan plan = Refinement.plan(template, base, addition, player.getEnchantmentSeed());
		preview(rows, player, template, base, addition, null, plan);
		List<Refinement.Power> powers = Refinement.powersOf(base);
		rows.add(new Heading(text("section.powers")));
		if (!powers.isEmpty() && powers.getFirst().perk() != null) {
			Refinement.Power power = powers.getFirst();
			rows.add(new Icon(template(power.trim()), power.perk().displayName(), Theme.perkColor(power.perk().id()),
				description("refinement.bcenchantments.shield." + power.perk().id()), Theme.TEXT_MUTED));
			rows.add(new Text(text("shield_one"), Theme.TEXT_FAINT));
			return;
		}
		rows.add(new Text(text("shield_none"), Theme.TEXT_BODY));
		rows.add(new Space(2));
		for (Refinement.Perk perk : Refinement.Perk.values()) {
			for (String trim : Refinement.shieldTrims(perk)) {
				boolean has = carries(player, trim);
				Component title = Component.empty().append(perk.displayName()).append(" · " + Refinement.trimName(trim));
				rows.add(new Icon(template(trim), title, has ? Theme.TEXT_BRIGHT : Theme.TEXT_MUTED,
					description("refinement.bcenchantments.shield." + perk.id()), has ? Theme.TEXT_MUTED : Theme.TEXT_FAINT));
			}
		}
		rows.add(new Text(text("how_trim"), Theme.TEXT_FAINT));
	}

	// ---- Helpers ----

	private static Component text(String key, Object... args) {
		return Component.translatable("panel.bcenchantments." + key, args);
	}

	/** A tooltip description, its line breaks joined so the panel can wrap it to its own width. */
	private static Component description(String key) {
		return Component.literal(Component.translatable(key).getString().replace('\n', ' '));
	}

	private static Component numeral(int level) {
		return Component.translatable("enchantment.level." + level);
	}

	private static Component levelName(int level) {
		return level == 0 ? Component.translatable("refinement.bcenchantments.unrefined")
			: Component.translatable("refinement.bcenchantments.level", numeral(level));
	}

	private static ItemStack template(String trim) {
		Item item = BuiltInRegistries.ITEM.getValue(Refinement.template(trim));
		return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
	}

	private static boolean carries(Player player, String trim) {
		Item item = BuiltInRegistries.ITEM.getValue(Refinement.template(trim));
		return player.hasInfiniteMaterials() || player.getInventory().contains(stack -> stack.is(item));
	}

	private static void slot(ItemStack stack, int x, int y) {
		Theme.box(g, x, y, x + SLOT, y + SLOT, Theme.argb(alpha * Theme.PANEL_ALPHA, Theme.SUNKEN));
		Theme.hairline(g, x, y, x + SLOT, y + SLOT, Theme.argb(alpha, Theme.BORDER_CONTROL));
		if (stack.isEmpty() || alpha < 0.5F) {
			return;
		}
		g.item(stack, x + 1, y + 1);
		if (mouseX >= x && mouseX < x + SLOT && mouseY >= y && mouseY < y + SLOT) {
			hovered = stack;
		}
	}

	private static void lines(Component text, int x, int y, int width, int color) {
		List<FormattedCharSequence> lines = font.split(text, width);
		for (int i = 0; i < lines.size(); i++) {
			g.text(font, lines.get(i), x, y + i * LINE, Theme.argb(alpha, color), false);
		}
	}

	private static Component ellipsize(Component text, int width) {
		if (font.width(text) <= width) {
			return text;
		}
		return Component.literal(font.plainSubstrByWidth(text.getString(), width - font.width("…")) + "…");
	}
}
