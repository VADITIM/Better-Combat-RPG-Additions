package dev.vadim.bcenchantments.client;

import dev.vadim.bcenchantments.combat.CriticalHits;
import dev.vadim.bcenchantments.network.PowerLevelPayload;
import dev.vadim.bcenchantments.network.SpendPointPayload;
import dev.vadim.bcenchantments.perk.Synergy;
import dev.vadim.bcenchantments.progression.Refinement;
import dev.vadim.bcenchantments.ui.Ctx;
import dev.vadim.bcenchantments.ui.Rich;
import dev.vadim.bcenchantments.ui.Row;
import dev.vadim.bcenchantments.ui.Rows;
import dev.vadim.bcenchantments.ui.SmithingView;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.SmithingScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
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
 * <p>
 * This class decides what the panel says; how it looks (rows, spacing, colours, text sizes) lives in the
 * shared {@code ui} package and {@code ui/style.json}, which the UI lab edits.
 */
public final class SmithingPanel {
	private static final int SCROLL_STEP = 20;
	private static final DecimalFormat NUMBER = new DecimalFormat("0.##");
	/** The player's own attack damage, attack speed and critical damage, which a weapon adds to. */
	private static final double BASE_DAMAGE = 1.0;
	private static final double BASE_SPEED = 4.0;
	private static final double BASE_CRIT_DAMAGE = 1.5;

	/** Whether the panel is shown; it stays as the player left it, like the recipe book. */
	private static boolean open = true;
	private static long openedAt;
	private static long changedAt;
	private static final SmithingView.Scroll scroll = new SmithingView.Scroll();
	private static int @Nullable [] bounds;
	private static ItemStack shown = ItemStack.EMPTY;
	/** The trim whose combinations are listed in full, if one was clicked. */
	private static Refinement.@Nullable Perk expanded;
	/** The trim of the power whose Degrade was clicked once and now asks to be confirmed. */
	private static @Nullable String confirming;
	private static int @Nullable [] view;

	private static final GuiCanvas canvas = new GuiCanvas();
	private static final Ctx ctx = new Ctx(canvas, UiSheet.get());

	private SmithingPanel() {
	}

	/** Moves the scroll wheel to the panel while the mouse is over it, and starts each table fresh. */
	static void register() {
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (!(screen instanceof SmithingScreen)) {
				return;
			}
			openedAt = Util.getMillis();
			changedAt = openedAt;
			scroll.value = 0.0F;
			shown = ItemStack.EMPTY;
			bounds = null;
			expanded = null;
			confirming = null;
			ctx.hits.clear();
			ScreenMouseEvents.allowMouseScroll(screen).register((s, x, y, horizontal, vertical) -> {
				if (!contains(x, y)) {
					return true;
				}
				scroll.value = Mth.clamp(scroll.value - (float) vertical * SCROLL_STEP, 0.0F, scroll.max);
				return false;
			});
			ScreenMouseEvents.allowMouseClick(screen).register((s, event) -> {
				int[] v = view;
				if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT || v == null || event.x() < v[0] || event.x() >= v[2] || event.y() < v[1] || event.y() >= v[3]) {
					return true;
				}
				for (Ctx.Hit hit : ctx.hits) {
					if (hit.contains(event.x(), event.y())) {
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
		return SmithingView.width(UiSheet.get(), screenWidth, open);
	}

	/** The table's left edge with the panel open: panel and table centred together, like the recipe book. */
	public static int tableLeft(int screenWidth) {
		return SmithingView.tableLeft(UiSheet.get(), screenWidth, open);
	}

	public static void extract(SmithingScreen screen, GuiGraphicsExtractor graphics, int mx, int my, int left, int top) {
		Player player = Minecraft.getInstance().player;
		int w = width(screen.width);
		ctx.begin(mx, my);
		if (player == null || w == 0) {
			bounds = null;
			view = null;
			return;
		}
		int[] panel = SmithingView.bounds(UiSheet.get(), screen.width, screen.height, open, left, top);
		bounds = panel;

		ItemStack template = screen.getMenu().getSlot(0).getItem();
		ItemStack base = screen.getMenu().getSlot(1).getItem();
		ItemStack addition = screen.getMenu().getSlot(2).getItem();
		long now = Util.getMillis();
		if (!ItemStack.isSameItemSameComponents(shown, base)) {
			if (shown.getItem() != base.getItem()) {
				scroll.value = 0.0F;
			}
			shown = base.copy();
			changedAt = now;
			confirming = null;
		}

		canvas.bind(graphics, Minecraft.getInstance().font);
		float panelAlpha = Theme.easeOut((now - openedAt) / (float) Theme.ENTER_MS);
		float settle = Theme.easeOut((now - changedAt) / 250.0F);
		view = SmithingView.draw(ctx, rows(player, template, base, addition), panel, panelAlpha, settle, scroll);
		if (ctx.hovered != null && ctx.hovered.payload() instanceof ItemStack stack) {
			graphics.setTooltipForNextFrame(Minecraft.getInstance().font, stack, mx, my);
		}
	}

	// ---- Content ----

	private static List<Row> rows(Player player, ItemStack template, ItemStack base, ItemStack addition) {
		List<Row> rows = new ArrayList<>();
		if (base.isEmpty()) {
			rows.add(new Rows.Text(text("empty"), "body"));
			rows.add(new Rows.Space(6));
			rows.add(new Rows.Text(text("empty.steps"), "muted"));
			return rows;
		}
		if (Refinement.isShield(base)) {
			shield(rows, player, template, base, addition);
		} else if (Refinement.isRefinable(base)) {
			weapon(rows, player, template, base, addition);
		} else if (Refinement.isWeapon(base)) {
			rows.add(new Rows.Header(UiText.ref(base), UiText.rich(base.getHoverName()), Rich.EMPTY));
			rows.add(new Rows.Text(text("why.tier"), "muted"));
		} else {
			rows.add(new Rows.Header(UiText.ref(base), UiText.rich(base.getHoverName()), Rich.EMPTY));
			rows.add(new Rows.Text(text("other"), "muted"));
		}
		return rows;
	}

	private static void weapon(List<Row> rows, Player player, ItemStack template, ItemStack base, ItemStack addition) {
		int seed = player.getEnchantmentSeed();
		Refinement refinement = Refinement.current(base, seed);
		Refinement.Plan plan = Refinement.plan(template, base, addition, seed);
		ItemStack result = plan == null ? ItemStack.EMPTY : plan.result();
		rows.add(new Rows.Header(UiText.ref(base), UiText.rich(base.getHoverName()), UiText.rich(levelName(refinement.level()))));

		preview(rows, player, template, base, addition, refinement, plan);

		rows.add(new Rows.Heading(text("section.stats")));
		stat(rows, "damage", base, result, stack -> BASE_DAMAGE + sum(stack, Attributes.ATTACK_DAMAGE), "");
		stat(rows, "speed", base, result, stack -> BASE_SPEED + sum(stack, Attributes.ATTACK_SPEED), "");
		stat(rows, "crit_chance", base, result, stack -> sum(stack, CriticalHits.CRITICAL_CHANCE) * 100.0, "%");
		stat(rows, "crit_damage", base, result, stack -> BASE_CRIT_DAMAGE + sum(stack, CriticalHits.CRITICAL_DAMAGE), "×");
		points(rows, base);

		if (refinement.level() < Refinement.MAX_LEVEL) {
			refining(rows, player, refinement, plan);
		} else {
			powers(rows, player, template, base, addition);
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
		List<Rich> lines = new ArrayList<>();
		boolean blocked = false;
		int cost = 0;
		String trim = Refinement.trimOf(template);
		if (plan != null) {
			if (trim == null) {
				// Emeralds paid in: the refinement bar already shows it.
				return;
			}
			Refinement.Perk perk = Refinement.perk(trim);
			int owned = Refinement.isShield(base) ? 0 : Refinement.perkLevel(base, perk.enchantment());
			if (owned > 0) {
				lines.add(text("preview.raise", tinted(perk), numeral(owned + 1)));
			} else {
				lines.add(text("preview.add", tinted(perk)));
				Refinement.Perk waiting = Refinement.unpaired(base);
				if (waiting != null && !Refinement.isShield(base)) {
					Synergy synergy = Synergy.of(waiting.enchantment(), perk.enchantment());
					if (synergy == null) {
						lines.add(text("preview.no_combination", tinted(waiting)));
					} else {
						lines.add(text("preview.combines", tinted(waiting)));
						lines.add(text("preview.into", synergy.displayName()));
					}
				}
				String locked = Refinement.lockedReason(perk, base, Refinement.powersOf(base));
				if (locked != null) {
					lines.add(UiText.rich(Component.translatable(locked)));
					blocked = true;
				}
			}
			cost = plan.levelCost();
			blocked |= cost > 0 && !player.hasInfiniteMaterials() && player.experienceLevel < cost;
		} else {
			Rich why = why(template, base, addition, refinement);
			if (why == null) {
				return;
			}
			lines.add(why);
			blocked = true;
		}
		for (Rich line : lines) {
			rows.add(new Rows.Text(line, blocked ? "danger" : "bright"));
		}
		if (cost > 0) {
			rows.add(new Rows.Space(UiSheet.get().num("ui.section_gap", 6)));
			rows.add(new Rows.Centered(text("preview.cost", cost), blocked ? "danger" : "bright"));
			rows.add(new Rows.Space(UiSheet.get().num("ui.section_gap", 6)));
		}
	}

	/** Why the table's inputs make nothing, or null if there is nothing useful to say. */
	private static @Nullable Rich why(ItemStack template, ItemStack base, ItemStack addition, @Nullable Refinement refinement) {
		String trim = Refinement.trimOf(template);
		if (Refinement.isShield(base)) {
			if (trim == null) {
				return null;
			}
			Refinement.Perk perk = Refinement.perk(trim);
			if (!Refinement.onShield(perk)) {
				return UiText.rich(Component.translatable("forge.bcenchantments.locked.shield"));
			}
			if (!Refinement.powersOf(base).isEmpty()) {
				return UiText.rich(Component.translatable("forge.bcenchantments.locked.shield_one"));
			}
			return addition.is(Items.ECHO_SHARD) ? null : text("why.shard");
		}
		if (refinement == null) {
			return null;
		}
		if (template.isEmpty() && Refinement.gemValue(addition) > 0) {
			if (refinement.level() < Refinement.MAX_LEVEL) {
				return null;
			}
			return Refinement.powersOf(base).isEmpty() ? text("why.refined_bare") : text("why.refined");
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
			return text("why.max", tinted(perk));
		}
		if (owned > 0) {
			return text("why.upgrade");
		}
		return addition.is(Items.ECHO_SHARD) ? null : text("why.shard");
	}

	private interface StatOf {
		double of(ItemStack stack);
	}

	private static void stat(List<Row> rows, String name, ItemStack base, ItemStack result, StatOf stat, String unit) {
		double now = stat.of(base);
		Rich next = null;
		if (!result.isEmpty()) {
			double then = stat.of(result);
			if (Math.abs(then - now) > 1.0E-4) {
				next = Rich.of(format(then, unit));
			}
		}
		rows.add(new Rows.Stat(text("stat." + name), Rich.of(format(now, unit)), next));
	}

	/** The weapon's level points: one per refinement level, each spent on the stat the player clicks. */
	private static void points(List<Row> rows, ItemStack base) {
		Refinement refinement = base.get(Refinement.COMPONENT);
		if (refinement == null || refinement.level() == 0) {
			return;
		}
		int left = refinement.unspentPoints();
		rows.add(new Rows.Heading(text("section.points")));
		rows.add(new Rows.Text(left > 0 ? text("points.left", left) : text("points.none"), left > 0 ? "bright" : "muted"));
		rows.add(new Rows.Space(2));
		for (Refinement.Stat stat : Refinement.Stat.values()) {
			int spent = refinement.points().of(stat);
			String value = spent > 0 ? "+" + NUMBER.format(spent * Refinement.POINT_BOOST * 100.0) + "%" : "";
			rows.add(new Rows.PointRow(text("stat." + stat.id()), Rich.of(value), left > 0,
				() -> ClientPlayNetworking.send(new SpendPointPayload(stat.id()))));
		}
		if (left > 0) {
			rows.add(new Rows.Text(text("points.how"), "faint"));
		}
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
		rows.add(new Rows.Heading(text("section.refinement")));
		int level = refinement.level();
		rows.add(new Rows.Text(text("level_of", numeral(level + 1), numeral(Refinement.MAX_LEVEL)), "body"));
		int incoming = 0;
		if (plan != null && plan.result().get(Refinement.COMPONENT) instanceof Refinement after) {
			incoming = after.level() > level ? refinement.cost() - refinement.gems() : after.gems() - refinement.gems();
		}
		rows.add(new Rows.Space(2));
		rows.add(new Rows.Bar(refinement.gems(), incoming, refinement.cost()));
		rows.add(new Rows.Text(text("gems", refinement.gems() + incoming, refinement.cost()), "muted"));
		if (level + 1 == Refinement.MAX_LEVEL) {
			rows.add(new Rows.Space(4));
			rows.add(new Rows.Text(text("unlocks"), "bright"));
		}
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

	private static void powers(List<Row> rows, Player player, ItemStack onTable, ItemStack base, ItemStack addition) {
		rows.add(new Rows.Heading(text("section.powers")));
		List<Refinement.Power> powers = Refinement.powersOf(base);
		if (powers.isEmpty()) {
			rows.add(new Rows.Text(text("no_powers"), "body"));
			return;
		}
		// What Upgrade already put on the table still counts, so the buttons stay usable.
		int gems = emeralds(player) + Refinement.gemValue(addition) * addition.getCount();
		boolean rich = player.hasInfiniteMaterials() || gems >= Refinement.UPGRADE_COST;
		for (Refinement.Power power : powers) {
			Refinement.Perk perk = power.perk();
			if (perk == null) {
				continue;
			}
			Rich name = UiText.rich(Component.empty().append(perk.displayName()).append(" ").append(numeral(power.level())));
			boolean highest = power.level() >= Refinement.MAX_PERK;
			rows.add(perkIcon(template(power.trim()), name, power.trim(), highest, description("tooltip.bcenchantments.power." + perk.id())));
			boolean template = carries(player, power.trim()) || !onTable.isEmpty() && onTable.is(template(power.trim()).getItem());
			boolean confirm = power.trim().equals(confirming);
			rows.add(new Rows.PowerButtons(text("downgrade"), text("degrade_confirm"), text("upgrade"), text("degrade_warning"),
				power.level() > 1, template && rich && !highest, confirm,
				() -> {
					if (confirm) {
						confirming = null;
						ClientPlayNetworking.send(new PowerLevelPayload(power.trim(), false));
					} else {
						confirming = power.trim();
					}
				},
				() -> {
					confirming = null;
					ClientPlayNetworking.send(new PowerLevelPayload(power.trim(), true));
				}));
			rows.add(new Rows.Space(3));
		}
	}

	/** A power's icon row: its name in its trim's colour (orange at its highest level) and what it does under it. */
	private static Rows.Icon perkIcon(ItemStack template, Rich name, String trim, boolean highest, Rich sub) {
		int rgb = Theme.trimColor(trim);
		String tone = highest ? "max" : "bright";
		return new Rows.Icon(UiText.ref(template), name, tone, highest || rgb == Theme.TEXT_BRIGHT ? -1 : rgb, sub, "muted");
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
		rows.add(new Rows.Title(text("section.pairs")));
		rows.add(new Rows.Space(8));
		for (int i = 0; i + 1 < powers.size(); i += 2) {
			Refinement.Perk first = powers.get(i).perk();
			Refinement.Perk second = powers.get(i + 1).perk();
			if (first == null || second == null) {
				continue;
			}
			Synergy synergy = Synergy.of(first.enchantment(), second.enchantment());
			Rich both = text("pair", tinted(first), tinted(second));
			if (synergy == null) {
				rows.add(new Rows.Text(both, "muted"));
				rows.add(new Rows.Text(text("no_combination"), "faint", 6));
			} else {
				rows.add(new Rows.Pair(both, UiText.rich(synergy.displayName())));
				rows.add(new Rows.Text(description("tooltip.bcenchantments.synergy." + synergy.id()), "muted", 6));
			}
			rows.add(new Rows.Space(5));
		}
		if (powers.size() % 2 == 1 && powers.getLast().perk() != null) {
			rows.add(new Rows.Text(text("alone", tinted(powers.getLast().perk())), "faint"));
		}
		for (Synergy synergy : Synergy.values()) {
			if (synergy.perks().size() != 2 && synergy.on(base)) {
				rows.add(new Rows.Text(UiText.rich(synergy.displayName()), "bright"));
				rows.add(new Rows.Text(description("tooltip.bcenchantments.synergy." + synergy.id()), "muted", 6));
			}
		}
	}

	/** What the next power costs and which trims would make a combination with what is on the weapon. */
	private static void next(List<Row> rows, Player player, ItemStack base) {
		List<Refinement.Power> powers = Refinement.powersOf(base);
		rows.add(new Rows.Space(UiSheet.get().num("ui.section_gap", 6)));
		int cost = Refinement.powerCost(powers.size());
		int missing = player.hasInfiniteMaterials() ? 0 : cost - player.experienceLevel;
		rows.add(missing > 0 ? new Rows.Centered(text("levels_missing", missing), "missing") : new Rows.Centered(text("levels_enough"), "enough"));
		rows.add(new Rows.Text(text("how_trim"), "faint"));

		rows.add(new Rows.Heading(text("section.possible")));
		Refinement.Perk waiting = Refinement.unpaired(base);
		if (waiting != null) {
			rows.add(new Rows.Text(text("waiting", tinted(waiting)), "body"));
			rows.add(new Rows.Space(2));
			boolean any = false;
			for (Refinement.Perk partner : candidates(base)) {
				Synergy synergy = Synergy.of(waiting.enchantment(), partner.enchantment());
				if (synergy == null) {
					continue;
				}
				any = true;
				String trim = Refinement.trim(partner);
				boolean has = carries(player, trim);
				rows.add(new Rows.Icon(UiText.ref(template(trim)), UiText.rich(synergy.displayName()), has ? "bright" : "muted", -1,
					text("from", Refinement.trimName(trim), tinted(partner)), has ? "muted" : "faint"));
			}
			if (!any) {
				rows.add(new Rows.Text(text("no_partner"), "faint"));
			}
			return;
		}
		rows.add(new Rows.Space(2));
		boolean any = false;
		List<Refinement.Perk> candidates = candidates(base);
		for (Refinement.Perk perk : candidates) {
			String trim = Refinement.trim(perk);
			if (!carries(player, trim)) {
				continue;
			}
			any = true;
			// What the power does on its own; a click lists what it combines into.
			Rows.Icon icon = perkIcon(template(trim), UiText.rich(perk.displayName()), trim, false,
				description("tooltip.bcenchantments.power." + perk.id()));
			rows.add(new Rows.TrimRow(icon, expanded == perk, () -> expanded = expanded == perk ? null : perk));
			if (expanded == perk) {
				combinations(rows, perk, candidates);
			}
			rows.add(new Rows.Space(UiSheet.get().num("ui.trim_gap", 8)));
		}
		if (!any) {
			rows.add(new Rows.Text(text("none_carried"), "faint"));
		}
	}

	/**
	 * Every combination {@code perk} is part of, not only the ones this weapon could still make: each with
	 * its partner, the partner's trim and what it does. Partners this weapon can no longer take are faint.
	 */
	private static void combinations(List<Row> rows, Refinement.Perk perk, List<Refinement.Perk> candidates) {
		int indent = UiSheet.get().num("icon.slot.size", 18) + UiSheet.get().num("icon.gap", 4);
		rows.add(new Rows.Space(3));
		boolean any = false;
		for (Refinement.Perk partner : Refinement.Perk.values()) {
			Synergy synergy = partner == perk || !partner.built() ? null : Synergy.of(perk.enchantment(), partner.enchantment());
			if (synergy == null) {
				continue;
			}
			any = true;
			boolean open = candidates.contains(partner);
			rows.add(new Rows.Text(UiText.rich(synergy.displayName()), open ? "bright" : "muted", indent));
			rows.add(new Rows.Text(text("combo_with", tinted(partner), Refinement.trimName(Refinement.trim(partner))),
				open ? "body" : "faint", indent));
			rows.add(new Rows.Text(description("tooltip.bcenchantments.synergy." + synergy.id()), open ? "muted" : "faint", indent));
			rows.add(new Rows.Space(4));
		}
		if (!any) {
			rows.add(new Rows.Text(text("no_combinations"), "faint", indent));
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
		rows.add(new Rows.Header(UiText.ref(base), UiText.rich(base.getHoverName()), text("shield")));
		Refinement.Plan plan = Refinement.plan(template, base, addition, player.getEnchantmentSeed());
		preview(rows, player, template, base, addition, null, plan);
		List<Refinement.Power> powers = Refinement.powersOf(base);
		rows.add(new Rows.Heading(text("section.powers")));
		if (!powers.isEmpty() && powers.getFirst().perk() != null) {
			Refinement.Power power = powers.getFirst();
			rows.add(perkIcon(template(power.trim()), UiText.rich(power.perk().displayName()), power.trim(), false,
				description("refinement.bcenchantments.shield." + power.perk().id())));
			rows.add(new Rows.Text(text("shield_one"), "faint"));
			return;
		}
		rows.add(new Rows.Text(text("shield_none"), "body"));
		rows.add(new Rows.Space(2));
		for (Refinement.Perk perk : Refinement.Perk.values()) {
			for (String trim : Refinement.shieldTrims(perk)) {
				boolean has = carries(player, trim);
				Rich title = UiText.rich(Component.empty().append(Refinement.tintedName(perk, trim)).append(" · " + Refinement.trimName(trim)));
				rows.add(new Rows.Icon(UiText.ref(template(trim)), title, has ? "bright" : "muted", -1,
					description("refinement.bcenchantments.shield." + perk.id()), has ? "muted" : "faint"));
			}
		}
		rows.add(new Rows.Text(text("how_trim"), "faint"));
	}

	// ---- Helpers ----

	private static Rich text(String key, Object... args) {
		return UiText.rich(Component.translatable("panel.bcenchantments." + key, args));
	}

	/** A tooltip description, its line breaks joined so the panel can wrap it to its own width. */
	private static Rich description(String key) {
		return Rich.keyed(Component.translatable(key).getString().replace('\n', ' '), key, List.of());
	}

	/** A power's name in the colour of the trim that grants it. */
	private static Component tinted(Refinement.Perk perk) {
		return Refinement.tintedName(perk, Refinement.trim(perk));
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
}
