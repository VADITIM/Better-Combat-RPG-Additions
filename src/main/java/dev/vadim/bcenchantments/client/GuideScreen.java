package dev.vadim.bcenchantments.client;

import dev.vadim.bcenchantments.perk.Perks;
import dev.vadim.bcenchantments.perk.Synergy;
import dev.vadim.bcenchantments.progression.Refinement;
import dev.vadim.bcenchantments.ui.Ctx;
import dev.vadim.bcenchantments.ui.GuideView;
import dev.vadim.bcenchantments.ui.ItemRef;
import dev.vadim.bcenchantments.ui.Rich;
import dev.vadim.bcenchantments.ui.Row;
import dev.vadim.bcenchantments.ui.Rows;
import dev.vadim.bcenchantments.ui.SmithingView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import org.lwjgl.glfw.GLFW;

/**
 * The player's guide, opened from the "?" button beside the smithing table: how to refine weapons,
 * what each power does, which trim grants which power and which powers combine. Each tab is a column of
 * blocks drawn with real item icons, the smithing recipes laid out slot by slot; hovering an icon shows
 * its tooltip. Text comes from the {@code guide.bcenchantments.*} lang entries. Closing it returns to
 * the smithing table, which stays open the whole time.
 * <p>
 * This class decides what each tab says; how it looks lives in the shared {@code ui} package and
 * {@code ui/style.json}, which the UI lab edits.
 */
public final class GuideScreen extends Screen {
	private static final int SCROLL_STEP = 20;

	private enum Tab {
		REFINING, PERKS, TRIMS, COMBINING;

		Rich label() {
			return UiText.rich(Component.translatable("guide.bcenchantments.tab." + name().toLowerCase(Locale.ROOT)));
		}
	}

	private final Screen parent;
	private final List<Row>[] tabs;
	private final SmithingView.Scroll[] scroll = new SmithingView.Scroll[Tab.values().length];
	private final GuiCanvas canvas = new GuiCanvas();
	private final Ctx ctx = new Ctx(canvas, UiSheet.get());
	private Tab tab = Tab.REFINING;
	private long openedAt = -1;
	private long tabAt = -1;

	@SuppressWarnings("unchecked")
	private GuideScreen(Screen parent) {
		super(Component.translatable("guide.bcenchantments.title"));
		this.parent = parent;
		this.tabs = new List[Tab.values().length];
		for (int i = 0; i < scroll.length; i++) {
			scroll[i] = new SmithingView.Scroll();
		}
	}

	/** The guide's button, which the smithing table puts beside itself ({@code SmithingScreenMixin}). */
	public static Button button(Screen parent, int x, int y) {
		return Button.builder(Component.literal("?"), button -> Minecraft.getInstance().gui.setScreen(new GuideScreen(parent)))
			.bounds(x, y, 20, 20)
			.tooltip(Tooltip.create(Component.translatable("guide.bcenchantments.open")))
			.build();
	}

	@Override
	protected void init() {
		if (openedAt < 0) {
			openedAt = Util.getMillis();
			tabAt = openedAt;
		}
	}

	private List<Row> blocks(Tab tab) {
		List<Row> blocks = tabs[tab.ordinal()];
		if (blocks == null) {
			blocks = switch (tab) {
				case REFINING -> refining();
				case PERKS -> perks();
				case TRIMS -> trims();
				case COMBINING -> combining();
			};
			tabs[tab.ordinal()] = blocks;
		}
		return blocks;
	}

	private List<Row> refining() {
		ItemStack sword = new ItemStack(Items.IRON_SWORD);
		ItemStack refined = sword.copy();
		refined.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
		return List.of(
			new Rows.Paragraph(text("refining.intro")),
			new Rows.Recipes(List.of(
				new Rows.Recipe(ItemRef.EMPTY, UiText.ref(sword), UiText.ref(new ItemStack(Items.EMERALD, 32)), UiText.ref(refined), text("refining.emeralds")),
				new Rows.Recipe(ItemRef.EMPTY, UiText.ref(sword), UiText.ref(new ItemStack(Items.EMERALD_BLOCK, 8)), UiText.ref(refined), text("refining.blocks")))),
			new Rows.Paragraph(text("refining.levels")),
			new Rows.Paragraph(text("refining.points")),
			new Rows.Paragraph(text("refining.tooltip"))
		);
	}

	private List<Row> perks() {
		List<Row> blocks = new ArrayList<>();
		blocks.add(new Rows.Paragraph(text("perks.intro")));
		for (Refinement.Perk perk : Refinement.Perk.values()) {
			if (!perk.built()) {
				continue;
			}
			String id = perk.id();
			boolean charged = Perks.CHARGED.stream().anyMatch(charge -> charge.enchantment() == perk.enchantment());
			blocks.add(new Rows.Entry(List.of(icon(perk.enchantment())), UiText.rich(perk.displayName()),
				perkColor(perk), charged ? text("perks.charged") : null, text("perk." + id), List.of()));
		}
		return blocks;
	}

	private List<Row> trims() {
		ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
		sword.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
		ItemRef swordRef = UiText.ref(sword);
		ItemRef template = item(Refinement.template("bolt"));
		List<Row> blocks = new ArrayList<>();
		blocks.add(new Rows.Recipe(template, swordRef, UiText.ref(new ItemStack(Items.ECHO_SHARD)), swordRef, text("trims.first")));
		blocks.add(new Rows.Recipe(template, swordRef, UiText.ref(new ItemStack(Items.EMERALD, Refinement.UPGRADE_COST)), swordRef, text("trims.raise")));
		blocks.add(new Rows.Recipe(item(Refinement.template("flow")), swordRef, UiText.ref(new ItemStack(Items.ECHO_SHARD)), swordRef, text("trims.combine")));
		blocks.add(new Rows.Paragraph(text("trims.which")));
		for (Refinement.Perk perk : Refinement.Perk.values()) {
			String trim = Refinement.trim(perk);
			String id = perk.id();
			Rich subtitle = perk.built() ? null : text("trims.coming");
			blocks.add(new Rows.Entry(List.of(item(Refinement.template(trim))), UiText.rich(perk.displayName()),
				perkColor(perk), subtitle, Rich.of(Refinement.trimName(trim)), List.of()));
		}
		ItemStack shield = new ItemStack(Items.SHIELD);
		ItemStack trimmedShield = shield.copy();
		trimmedShield.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
		blocks.add(new Rows.Recipe(item(Refinement.template("flow")), UiText.ref(shield), UiText.ref(new ItemStack(Items.ECHO_SHARD)),
			UiText.ref(trimmedShield), text("trims.shield")));
		for (Refinement.Perk perk : Refinement.Perk.values()) {
			List<String> trims = Refinement.shieldTrims(perk);
			if (trims.isEmpty()) {
				continue;
			}
			List<ItemRef> templates = trims.stream().map(trim -> item(Refinement.template(trim))).toList();
			String id = perk.enchantment().identifier().getPath();
			blocks.add(new Rows.Entry(List.of(UiText.ref(trimmedShield)), UiText.rich(Component.translatable("enchantment.bcenchantments." + id)),
				perkColor(perk), text("trims.shield_power"), text("shield." + id), templates));
		}
		return blocks;
	}

	private List<Row> combining() {
		List<Row> blocks = new ArrayList<>();
		blocks.add(new Rows.Paragraph(text("combining.intro")));
		for (Synergy synergy : Synergy.values()) {
			List<ItemRef> books = new ArrayList<>(synergy.perks().stream().map(this::icon).toList());
			// Each power in its trim's colour.
			MutableComponent names = Component.empty();
			for (var key : synergy.perks()) {
				Refinement.Perk perk = Refinement.Perk.of(key);
				Component name = perk != null ? Refinement.tintedName(perk, Refinement.trim(perk))
					: Component.translatable("enchantment.bcenchantments." + key.identifier().getPath());
				names.append(names.getSiblings().isEmpty() ? Component.empty() : Component.literal(" + ")).append(name);
			}
			if (synergy.refined()) {
				ItemStack refined = new ItemStack(Items.IRON_SWORD);
				refined.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
				books.add(UiText.ref(refined));
				names.append(" + ").append(Component.translatable("guide.bcenchantments.combining.refined"));
			}
			blocks.add(new Rows.Entry(books, UiText.rich(synergy.displayName()), 0xFFAA00, UiText.rich(names),
				UiText.rich(Component.translatable("synergy.bcenchantments." + synergy.id() + ".desc")), List.of()));
		}
		return blocks;
	}

	/** A power's colour, its trim's, or -1 for the plain title colour. */
	private static int perkColor(Refinement.Perk perk) {
		int rgb = Theme.trimColor(perk);
		return rgb == Theme.TEXT_BRIGHT ? -1 : rgb;
	}

	private static Rich text(String key) {
		return UiText.rich(Component.translatable("guide.bcenchantments." + key));
	}

	private static ItemRef item(Identifier id) {
		Item item = BuiltInRegistries.ITEM.getValue(id);
		return item == Items.AIR ? ItemRef.EMPTY : UiText.ref(new ItemStack(item));
	}

	/** The trim template that grants {@code perk}. */
	private ItemRef icon(ResourceKey<Enchantment> perk) {
		Refinement.Perk power = Refinement.Perk.of(perk);
		return power == null ? UiText.ref(new ItemStack(Items.ENCHANTED_BOOK)) : item(Refinement.template(Refinement.trim(power)));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
		long now = Util.getMillis();
		ctx.begin(mouseX, mouseY);
		canvas.bind(g, font);
		float alpha = Theme.easeOut((now - openedAt) / (float) Theme.ENTER_MS);
		float switched = Theme.easeOut((now - tabAt) / 250.0F);
		List<Rich> labels = new ArrayList<>();
		for (Tab t : Tab.values()) {
			labels.add(t.label());
		}
		GuideView.draw(ctx, width, height, UiText.rich(getTitle()), text("close"), labels, tab.ordinal(), this::select,
			blocks(tab), scroll[tab.ordinal()], alpha, switched);
		if (ctx.hovered != null && ctx.hovered.payload() instanceof ItemStack stack) {
			g.setTooltipForNextFrame(font, stack, mouseX, mouseY);
		}
	}

	private void select(int index) {
		Tab picked = Tab.values()[index];
		if (picked != tab) {
			tab = picked;
			tabAt = Util.getMillis();
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
			for (Ctx.Hit hit : ctx.hits) {
				if (hit.contains(event.x(), event.y())) {
					hit.action().run();
					return true;
				}
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		SmithingView.Scroll s = scroll[tab.ordinal()];
		s.value = Mth.clamp(s.value - (float) scrollY * SCROLL_STEP, 0.0F, s.max);
		return true;
	}

	@Override
	public void onClose() {
		Minecraft.getInstance().gui.setScreen(parent);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
