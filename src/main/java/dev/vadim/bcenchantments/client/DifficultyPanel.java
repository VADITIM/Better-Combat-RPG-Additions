package dev.vadim.bcenchantments.client;

import dev.vadim.bcenchantments.mixin.AbstractContainerScreenAccessor;
import dev.vadim.bcenchantments.mob.Adaptation;
import dev.vadim.bcenchantments.mob.Landmark;
import dev.vadim.bcenchantments.mob.Omen;
import dev.vadim.bcenchantments.mob.Place;
import dev.vadim.bcenchantments.network.LandmarkPayload;
import dev.vadim.bcenchantments.ui.Ctx;
import dev.vadim.bcenchantments.ui.DifficultyView;
import dev.vadim.bcenchantments.ui.ItemRef;
import dev.vadim.bcenchantments.ui.Rich;
import dev.vadim.bcenchantments.ui.Row;
import dev.vadim.bcenchantments.ui.Rows;
import dev.vadim.bcenchantments.ui.SmithingView;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * The panel above the inventory: how hard the spot the player stands in is (structure or biome, its
 * tier and multiplier, Bad Omen), how much power the weapons they carry add and how much each one
 * counts, and which trim power monsters are adapting to and how many counter it. Its numbers come from
 * {@link Place} and {@link Adaptation}, the same code the server spawns with.
 * <p>
 * This class decides what the panel says; how it looks lives in the shared {@code ui} package
 * ({@link DifficultyView}) and {@code ui/style.json}.
 */
public final class DifficultyPanel {
	private static final int SCROLL_STEP = 10;
	private static final int MAX_WEAPONS = 6;
	private static final DecimalFormat NUMBER = new DecimalFormat("0.#");
	private static final String[] NUMERALS = {"I", "II", "III", "IV", "V"};

	/** The structure the server last said the player stands in; see {@link Landmark}. */
	private static @Nullable Landmark landmark;

	private static long openedAt;
	private static final SmithingView.Scroll scroll = new SmithingView.Scroll();
	private static final int[] bounds = new int[4];
	private static boolean shown;
	private static final GuiCanvas canvas = new GuiCanvas();
	private static final Ctx ctx = new Ctx(canvas, UiSheet.get());

	private DifficultyPanel() {
	}

	public static void receive(LandmarkPayload payload) {
		landmark = Landmark.byOrdinal(payload.landmark());
	}

	public static void reset() {
		landmark = null;
	}

	static void register() {
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (!(screen instanceof InventoryScreen inventory)) {
				return;
			}
			openedAt = Util.getMillis();
			scroll.value = 0.0F;
			shown = false;
			ScreenEvents.afterExtract(screen).register((s, g, mx, my, delta) -> extract(inventory, g, mx, my));
			ScreenMouseEvents.allowMouseScroll(screen).register((s, x, y, horizontal, vertical) -> {
				if (!contains(x, y)) {
					return true;
				}
				scroll.value = Mth.clamp(scroll.value - (float) vertical * SCROLL_STEP, 0.0F, scroll.max);
				return false;
			});
			// A click on the panel is not a click outside the inventory, so it does not throw the carried item.
			ScreenMouseEvents.allowMouseClick(screen).register((s, event) -> !contains(event.x(), event.y()));
		});
	}

	private static boolean contains(double x, double y) {
		return shown && x >= bounds[0] && x < bounds[2] && y >= bounds[1] && y < bounds[3];
	}

	private static void extract(InventoryScreen screen, GuiGraphicsExtractor g, int mx, int my) {
		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		ctx.begin(mx, my);
		if (player == null || mc.level == null) {
			shown = false;
			return;
		}
		AbstractContainerScreenAccessor at = (AbstractContainerScreenAccessor) screen;
		canvas.bind(g, mc.font);
		float alpha = Theme.easeOut((Util.getMillis() - openedAt) / (float) Theme.ENTER_MS);
		int[] view = DifficultyView.draw(ctx, rows(player), screen.width, at.bcenchantments$leftPos(), at.bcenchantments$topPos(),
			at.bcenchantments$imageWidth(), alpha, scroll, bounds);
		shown = view != null;
		if (ctx.hovered != null && ctx.hovered.payload() instanceof ItemStack stack) {
			g.setTooltipForNextFrame(mc.font, stack, mx, my);
		}
	}

	// ---- Content ----

	private static List<Row> rows(Player player) {
		List<Row> rows = new ArrayList<>();
		Place.Spot spot = Place.of(player.level(), player.blockPosition(), landmark);
		int omen = Omen.level(player);
		Place.Tier tier = Place.tier(spot, omen);
		Component where = landmark != null
			? Component.translatable(landmark.translationKey())
			: player.level().getBiome(player.blockPosition()).unwrapKey()
				.<Component>map(key -> Component.translatable(Util.makeDescriptionId("biome", key.identifier())))
				.orElse(Component.empty());
		rows.add(new Rows.Stat(UiText.rich(where),
			UiText.rich(Component.translatable("panel.bcenchantments.difficulty.tier", Component.translatable(tier.translationKey()),
				NUMBER.format(spot.multiplier()))), null));
		if (omen > 0) {
			rows.add(new Rows.Stat(text("omen"), Rich.of(NUMERALS[Math.min(omen, NUMERALS.length) - 1]), null));
		}

		Adaptation.Profile profile = Adaptation.of(player);
		rows.add(new Rows.Stat(text("power"), text("power.value", NUMBER.format(profile.power()), Adaptation.maxPower()), null));
		if (profile.weapons().isEmpty()) {
			rows.add(new Rows.Text(text("no_weapons"), "muted"));
		} else {
			List<ItemRef> items = new ArrayList<>();
			List<Rich> values = new ArrayList<>();
			for (Adaptation.Weapon weapon : profile.weapons().subList(0, Math.min(profile.weapons().size(), MAX_WEAPONS))) {
				items.add(UiText.ref(weapon.stack()));
				values.add(text("weapon", NUMBER.format(weapon.counted())));
			}
			rows.add(new Rows.Weapons(items, values));
		}

		if (profile.adapted() == null) {
			rows.add(new Rows.Text(text("not_adapted"), "muted"));
		} else {
			rows.add(new Rows.Stat(text("adapted"), UiText.rich(profile.adapted().displayName()), null));
			rows.add(new Rows.Stat(text("countering"), text("countering.value", Math.round(profile.counterShare() * 100.0),
				Math.round(Adaptation.maxCounterShare() * 100.0)), null));
		}
		return rows;
	}

	private static Rich text(String key, Object... args) {
		return UiText.rich(Component.translatable("panel.bcenchantments.difficulty." + key, args));
	}
}
