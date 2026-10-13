package dev.vadim.bcenchantments.client;

import dev.vadim.bcenchantments.perk.Perks;
import dev.vadim.bcenchantments.perk.Synergy;
import dev.vadim.bcenchantments.progression.Refinement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.SmithingScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.lwjgl.glfw.GLFW;

/**
 * The player's guide, opened from the "?" button beside the smithing table: how to refine weapons,
 * what each power does, which trim grants which power and which powers combine. Each tab is a column of
 * blocks drawn with real item icons, the smithing recipes laid out slot by slot; hovering an icon shows
 * its tooltip. Text comes from the {@code guide.bcenchantments.*} lang entries. Closing it returns to
 * the smithing table, which stays open the whole time.
 */
public final class GuideScreen extends Screen {
	private static final int MARGIN = 12;
	private static final int PADDING = 8;
	private static final int TITLE_HEIGHT = 16;
	private static final int TAB_HEIGHT = 14;
	private static final int SLOT = 18;
	private static final int GAP = 8;
	private static final int LINE = 10;
	private static final int SCROLL_STEP = 20;
	/** The smithing table's own size, which places the button beside it. */
	private static final int SMITHING_WIDTH = 176;
	private static final int SMITHING_HEIGHT = 166;

	private enum Tab {
		REFINING, PERKS, TRIMS, COMBINING;

		Component label() {
			return Component.translatable("guide.bcenchantments.tab." + name().toLowerCase(Locale.ROOT));
		}
	}

	/** One piece of a tab, laid out top to bottom. */
	private interface Block {
		int height(GuideScreen screen, int width);

		void draw(GuideScreen screen, GuiGraphicsExtractor g, int x, int y, int width, float alpha);
	}

	/** A wrapped paragraph in body text. */
	private record Paragraph(Component text) implements Block {
		@Override
		public int height(GuideScreen screen, int width) {
			return screen.font.split(text, width).size() * LINE;
		}

		@Override
		public void draw(GuideScreen screen, GuiGraphicsExtractor g, int x, int y, int width, float alpha) {
			screen.lines(g, text, x, y, width, Theme.argb(alpha, Theme.TEXT_BODY));
		}
	}

	/** A smithing table recipe, slot by slot as the table lays it out, with a caption under it. */
	private record Recipe(ItemStack template, ItemStack base, ItemStack addition, ItemStack result, Component caption) implements Block {
		@Override
		public int height(GuideScreen screen, int width) {
			return SLOT + 6 + screen.font.split(caption, width).size() * LINE;
		}

		@Override
		public void draw(GuideScreen screen, GuiGraphicsExtractor g, int x, int y, int width, float alpha) {
			int sx = x;
			ItemStack[] inputs = {template, base, addition};
			for (int i = 0; i < inputs.length; i++) {
				screen.slot(g, inputs[i], sx, y, alpha);
				sx += SLOT + 2;
			}
			sx += 4;
			g.text(screen.font, "→", sx, y + 5, Theme.argb(alpha, Theme.TEXT_MUTED), false);
			sx += screen.font.width("→") + 6;
			screen.slot(g, result, sx, y, alpha);
			screen.lines(g, caption, x, y + SLOT + 6, width, Theme.argb(alpha, Theme.TEXT_MUTED));
		}
	}

	/**
	 * Icons on the left, a coloured title with an optional subtitle beside them, a description under the
	 * title and an optional row of extra icons under that.
	 */
	private record Entry(List<ItemStack> icons, Component title, int titleColor, Component subtitle, Component body, List<ItemStack> row) implements Block {
		private int textX() {
			return icons.size() * (SLOT + 2) + 4;
		}

		@Override
		public int height(GuideScreen screen, int width) {
			int text = LINE + (subtitle == null ? 0 : LINE) + screen.font.split(body, width - textX()).size() * LINE;
			if (!row.isEmpty()) {
				text += 2 + SLOT;
			}
			return Math.max(SLOT, text);
		}

		@Override
		public void draw(GuideScreen screen, GuiGraphicsExtractor g, int x, int y, int width, float alpha) {
			for (int i = 0; i < icons.size(); i++) {
				screen.slot(g, icons.get(i), x + i * (SLOT + 2), y, alpha);
			}
			int tx = x + textX();
			int ty = y + 1;
			g.text(screen.font, title, tx, ty, Theme.argb(alpha, titleColor), true);
			ty += LINE;
			if (subtitle != null) {
				g.text(screen.font, subtitle, tx, ty, Theme.argb(alpha, Theme.TEXT_LABEL), false);
				ty += LINE;
			}
			ty += screen.lines(g, body, tx, ty, width - textX(), Theme.argb(alpha, Theme.TEXT_BODY));
			if (!row.isEmpty()) {
				ty += 2;
				for (int i = 0; i < row.size(); i++) {
					screen.slot(g, row.get(i), tx + i * (SLOT + 2), ty, alpha);
				}
			}
		}
	}

	private final Screen parent;
	private final List<Block>[] tabs;
	private final float[] scroll = new float[Tab.values().length];
	private final List<int[]> tabHits = new ArrayList<>();
	private Tab tab = Tab.REFINING;
	private long openedAt = -1;
	private long tabAt = -1;
	private int maxScroll;
	private int viewX0;
	private int viewY0;
	private int viewX1;
	private int viewY1;
	private int mouseX;
	private int mouseY;
	private ItemStack hovered = ItemStack.EMPTY;

	@SuppressWarnings("unchecked")
	private GuideScreen(Screen parent) {
		super(Component.translatable("guide.bcenchantments.title"));
		this.parent = parent;
		this.tabs = new List[Tab.values().length];
	}

	/** Puts the guide's button beside every smithing table. */
	static void register() {
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (!(screen instanceof SmithingScreen)) {
				return;
			}
			int left = (width - SMITHING_WIDTH) / 2;
			int top = (height - SMITHING_HEIGHT) / 2;
			Screens.getWidgets(screen).add(Button.builder(Component.literal("?"), button -> client.gui.setScreen(new GuideScreen(screen)))
				.bounds(left + SMITHING_WIDTH + 4, top, 20, 20)
				.tooltip(Tooltip.create(Component.translatable("guide.bcenchantments.open")))
				.build());
		});
	}

	@Override
	protected void init() {
		if (openedAt < 0) {
			openedAt = Util.getMillis();
			tabAt = openedAt;
		}
	}

	private List<Block> blocks(Tab tab) {
		List<Block> blocks = tabs[tab.ordinal()];
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

	private List<Block> refining() {
		ItemStack sword = new ItemStack(Items.IRON_SWORD);
		ItemStack refined = sword.copy();
		refined.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
		return List.of(
			new Paragraph(text("refining.intro")),
			new Recipe(ItemStack.EMPTY, sword, new ItemStack(Items.EMERALD, 32), refined, text("refining.emeralds")),
			new Recipe(ItemStack.EMPTY, sword, new ItemStack(Items.EMERALD_BLOCK, 8), refined, text("refining.blocks")),
			new Paragraph(text("refining.levels")),
			new Paragraph(text("refining.tooltip"))
		);
	}

	private List<Block> perks() {
		List<Block> blocks = new ArrayList<>();
		blocks.add(new Paragraph(text("perks.intro")));
		for (ResourceKey<Enchantment> perk : Perks.ALL) {
			String id = perk.identifier().getPath();
			boolean charged = Perks.CHARGED.stream().anyMatch(charge -> charge.enchantment() == perk);
			blocks.add(new Entry(List.of(book(perk)), Component.translatable("enchantment.bcenchantments." + id),
				Theme.perkColor(id), charged ? text("perks.charged") : null, text("perk." + id), List.of()));
		}
		return blocks;
	}

	private List<Block> trims() {
		ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
		sword.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
		ItemStack template = item(Refinement.template("bolt"));
		List<Block> blocks = new ArrayList<>();
		blocks.add(new Recipe(template, sword, new ItemStack(Items.ECHO_SHARD), sword, text("trims.first")));
		blocks.add(new Recipe(template, sword, ItemStack.EMPTY, sword, text("trims.raise")));
		blocks.add(new Paragraph(text("trims.which")));
		for (Refinement.Perk perk : Refinement.Perk.values()) {
			List<String> trims = Refinement.trims(perk);
			List<ItemStack> templates = trims.stream().map(trim -> item(Refinement.template(trim))).toList();
			String id = perk.enchantment().identifier().getPath();
			String names = String.join(", ", trims.stream().map(trim -> trim.substring(0, 1).toUpperCase(Locale.ROOT) + trim.substring(1)).toList());
			blocks.add(new Entry(List.of(book(perk.enchantment())), Component.translatable("enchantment.bcenchantments." + id),
				Theme.perkColor(id), null, Component.literal(names), templates));
		}
		ItemStack shield = new ItemStack(Items.SHIELD);
		ItemStack trimmedShield = shield.copy();
		trimmedShield.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
		blocks.add(new Recipe(item(Refinement.template("flow")), shield, new ItemStack(Items.ECHO_SHARD), trimmedShield, text("trims.shield")));
		for (Refinement.Perk perk : Refinement.Perk.values()) {
			List<String> trims = Refinement.shieldTrims(perk);
			if (trims.isEmpty()) {
				continue;
			}
			List<ItemStack> templates = trims.stream().map(trim -> item(Refinement.template(trim))).toList();
			String id = perk.enchantment().identifier().getPath();
			blocks.add(new Entry(List.of(trimmedShield), Component.translatable("enchantment.bcenchantments." + id),
				Theme.perkColor(id), text("trims.shield_power"), text("shield." + id), templates));
		}
		return blocks;
	}

	private List<Block> combining() {
		List<Block> blocks = new ArrayList<>();
		blocks.add(new Paragraph(text("combining.intro")));
		for (Synergy synergy : Synergy.values()) {
			List<ItemStack> books = new ArrayList<>(synergy.perks().stream().map(this::book).toList());
			List<String> names = new ArrayList<>(synergy.perks().stream()
				.map(perk -> Component.translatable("enchantment.bcenchantments." + perk.identifier().getPath()).getString()).toList());
			if (synergy.refined()) {
				ItemStack refined = new ItemStack(Items.IRON_SWORD);
				refined.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
				books.add(refined);
				names.add(text("combining.refined").getString());
			}
			blocks.add(new Entry(books, synergy.displayName(), 0xFFAA00, Component.literal(String.join(" + ", names)),
				Component.translatable("synergy.bcenchantments." + synergy.id() + ".desc"), List.of()));
		}
		return blocks;
	}

	private static Component text(String key) {
		return Component.translatable("guide.bcenchantments." + key);
	}

	private static ItemStack item(Identifier id) {
		Item item = BuiltInRegistries.ITEM.getValue(id);
		return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
	}

	/** An enchanted book holding {@code perk}, or a plain one if the world does not know it. */
	private ItemStack book(ResourceKey<Enchantment> perk) {
		if (minecraft == null || minecraft.level == null) {
			return new ItemStack(Items.ENCHANTED_BOOK);
		}
		return minecraft.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(perk)
			.map(holder -> EnchantmentHelper.createBook(new EnchantmentInstance(holder, 1)))
			.orElseGet(() -> new ItemStack(Items.ENCHANTED_BOOK));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
		long now = Util.getMillis();
		this.mouseX = mouseX;
		this.mouseY = mouseY;
		hovered = ItemStack.EMPTY;
		tabHits.clear();

		int panelWidth = Math.min(340, width - 2 * MARGIN);
		int panelHeight = Math.min(250, height - 2 * MARGIN);
		int x0 = (width - panelWidth) / 2;
		int y0 = (height - panelHeight) / 2;
		int x1 = x0 + panelWidth;
		int y1 = y0 + panelHeight;
		float alpha = Theme.easeOut((now - openedAt) / (float) Theme.ENTER_MS);
		Theme.panel(g, x0, y0, x1, y1, alpha, Theme.BORDER);

		int inner0 = x0 + PADDING;
		int inner1 = x1 - PADDING;
		int y = y0 + PADDING;
		g.text(font, getTitle(), inner0, y, Theme.argb(alpha, Theme.TEXT_BRIGHT), true);
		String hint = Component.translatable("guide.bcenchantments.close").getString();
		g.text(font, hint, inner1 - font.width(hint), y, Theme.argb(alpha, Theme.TEXT_FAINT), false);
		y += TITLE_HEIGHT;

		Tab[] all = Tab.values();
		int tabWidth = (inner1 - inner0 - (all.length - 1) * 2) / all.length;
		for (int i = 0; i < all.length; i++) {
			int tx0 = inner0 + i * (tabWidth + 2);
			int tx1 = i == all.length - 1 ? inner1 : tx0 + tabWidth;
			tab(g, all[i], tx0, y, tx1, alpha);
		}
		y += TAB_HEIGHT + PADDING;

		viewX0 = inner0;
		viewY0 = y;
		viewX1 = inner1;
		viewY1 = y1 - PADDING;
		content(g, now, alpha);
		if (!hovered.isEmpty()) {
			g.setTooltipForNextFrame(font, hovered, mouseX, mouseY);
		}
	}

	private void tab(GuiGraphicsExtractor g, Tab which, int x0, int y0, int x1, float alpha) {
		int y1 = y0 + TAB_HEIGHT;
		tabHits.add(new int[] {which.ordinal(), x0, y0, x1, y1});
		boolean selected = tab == which;
		boolean over = mouseX >= x0 && mouseX < x1 && mouseY >= y0 && mouseY < y1;
		Theme.box(g, x0, y0, x1, y1, selected ? Theme.argb(alpha, Theme.ACCENT) : Theme.argb(alpha * Theme.PANEL_ALPHA, Theme.SUNKEN));
		int ring = selected ? Theme.TEXT_MUTED : over ? Theme.BORDER_HOVER : Theme.BORDER_CONTROL;
		Theme.hairline(g, x0, y0, x1, y1, Theme.argb(alpha, ring));
		String label = which.label().getString();
		int color = selected ? Theme.ON_ACCENT : over ? Theme.TEXT_BODY : Theme.TEXT_MUTED;
		g.text(font, label, (x0 + x1 - font.width(label)) / 2, y0 + 3, Theme.argb(alpha, color), false);
	}

	/** The selected tab's blocks, clipped to the view and scrolled; they fade in after a tab switch. */
	private void content(GuiGraphicsExtractor g, long now, float alpha) {
		List<Block> blocks = blocks(tab);
		int width = viewX1 - viewX0 - 6;
		int total = -GAP;
		for (Block block : blocks) {
			total += block.height(this, width) + GAP;
		}
		maxScroll = Math.max(0, total - (viewY1 - viewY0));
		int index = tab.ordinal();
		scroll[index] = Mth.clamp(scroll[index], 0.0F, maxScroll);

		float fade = alpha * Theme.easeOut((now - tabAt) / 250.0F);
		float rise = (1.0F - Theme.easeOut((now - tabAt) / 250.0F)) * 6.0F;
		g.enableScissor(viewX0, viewY0, viewX1, viewY1);
		g.pose().pushMatrix();
		g.pose().translate(0.0F, rise);
		int y = viewY0 - Math.round(scroll[index]);
		for (Block block : blocks) {
			int h = block.height(this, width);
			if (y + h >= viewY0 && y <= viewY1) {
				block.draw(this, g, viewX0, y, width, fade);
			}
			y += h + GAP;
		}
		g.pose().popMatrix();
		g.disableScissor();

		if (maxScroll > 0) {
			int track = viewY1 - viewY0;
			int thumb = Math.max(12, track * track / (track + maxScroll));
			int ty = viewY0 + Math.round((track - thumb) * scroll[index] / maxScroll);
			g.fill(viewX1 - 2, viewY0, viewX1, viewY1, Theme.argb(alpha * 0.5F, Theme.BORDER));
			g.fill(viewX1 - 2, ty, viewX1, ty + thumb, Theme.argb(alpha, Theme.BORDER_HOVER));
		}
	}

	/** One item slot as the smithing table shows it; an empty stack leaves the slot empty. */
	private void slot(GuiGraphicsExtractor g, ItemStack stack, int x, int y, float alpha) {
		Theme.box(g, x, y, x + SLOT, y + SLOT, Theme.argb(alpha * Theme.PANEL_ALPHA, Theme.SUNKEN));
		Theme.hairline(g, x, y, x + SLOT, y + SLOT, Theme.argb(alpha, Theme.BORDER_CONTROL));
		if (stack.isEmpty() || alpha < 0.5F) {
			return;
		}
		g.item(stack, x + 1, y + 1);
		g.itemDecorations(font, stack, x + 1, y + 1);
		if (mouseX >= x && mouseX < x + SLOT && mouseY >= y && mouseY < y + SLOT
			&& mouseX >= viewX0 && mouseX < viewX1 && mouseY >= viewY0 && mouseY < viewY1) {
			hovered = stack;
		}
	}

	/** Draws {@code text} wrapped to {@code width} and returns the height it took. */
	private int lines(GuiGraphicsExtractor g, Component text, int x, int y, int width, int color) {
		List<FormattedCharSequence> lines = font.split(text, width);
		for (int i = 0; i < lines.size(); i++) {
			g.text(font, lines.get(i), x, y + i * LINE, color, false);
		}
		return lines.size() * LINE;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
			for (int[] hit : tabHits) {
				if (event.x() >= hit[1] && event.x() < hit[3] && event.y() >= hit[2] && event.y() < hit[4]) {
					Tab picked = Tab.values()[hit[0]];
					if (picked != tab) {
						tab = picked;
						tabAt = Util.getMillis();
					}
					return true;
				}
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		int index = tab.ordinal();
		scroll[index] = Mth.clamp(scroll[index] - (float) scrollY * SCROLL_STEP, 0.0F, maxScroll);
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
