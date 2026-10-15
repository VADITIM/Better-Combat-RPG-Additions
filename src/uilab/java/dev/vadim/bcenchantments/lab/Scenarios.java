package dev.vadim.bcenchantments.lab;

import dev.vadim.bcenchantments.ui.ItemRef;
import dev.vadim.bcenchantments.ui.Rich;
import dev.vadim.bcenchantments.ui.Row;
import dev.vadim.bcenchantments.ui.Rows;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Sample states for the smithing panel, the Field Guide and the inventory's difficulty panel, built from the same rows the mod builds and
 * the real lang entries. They stand in for the game state the mod reads; the mod decides which rows a
 * real weapon gets, the lab only has to show every kind of row in believable company.
 */
final class Scenarios {
	/** What a click changes in a sample: the trim row that is open, the guide tab. */
	static final class State {
		String expanded;
		int tab;
	}

	/** One smithing table setup: what lies in its slots and the rows the panel shows for it. */
	record Smithing(String name, ItemRef template, ItemRef base, ItemRef addition, ItemRef result, Function<State, List<Row>> rows) {
	}

	private record PerkInfo(String id, String trim, int rgb, boolean built, boolean charged) {
	}

	private static final List<PerkInfo> PERKS = List.of(
		new PerkInfo("lightning", "bolt", 0xFFDD1B, true, true),
		new PerkInfo("shockwave", "ward", 0x2EE6FF, true, true),
		new PerkInfo("cold_edge", "tide", -1, true, false),
		new PerkInfo("uppercut", "raiser", -1, true, false),
		new PerkInfo("chains", "sentry", 0x9FB0BE, true, true),
		new PerkInfo("echo", "silence", 0xA98BFF, true, true),
		new PerkInfo("wind_charged", "flow", -1, true, false),
		new PerkInfo("gravity", "eye", -1, true, false),
		new PerkInfo("marked", "wayfinder", -1, true, false),
		new PerkInfo("rampaging", "snout", -1, true, false),
		new PerkInfo("venomous", "wild", -1, true, false),
		new PerkInfo("curse", "dune", -1, true, false),
		new PerkInfo("plunder", "coast", -1, true, false),
		new PerkInfo("withering", "rib", -1, true, true),
		new PerkInfo("shulker_burst", "spire", -1, true, true),
		new PerkInfo("shared_pain", "host", -1, true, false),
		new PerkInfo("guardian_strike", "vex", -1, true, false),
		new PerkInfo("copper_shock", "shaper", -1, true, false));

	private record Combo(String id, String a, String b) {
	}

	private static final List<Combo> COMBOS = List.of(
		new Combo("chained_lightning", "chains", "lightning"), new Combo("charged_lightning", "lightning", "wind_charged"),
		new Combo("updraft", "uppercut", "wind_charged"), new Combo("frozen_chains", "chains", "cold_edge"),
		new Combo("frost_burst", "cold_edge", "wind_charged"), new Combo("resonance", "echo", "lightning"),
		new Combo("tempest", "shockwave", "wind_charged"), new Combo("frenzy", "echo", null),
		new Combo("shatter", "cold_edge", "lightning"), new Combo("singularity", "gravity", "shockwave"),
		new Combo("black_hole", "gravity", "chains"), new Combo("cyclone", "gravity", "wind_charged"),
		new Combo("launch", "marked", "uppercut"), new Combo("blast_step", "marked", "wind_charged"),
		new Combo("thunderwave", "lightning", "shockwave"), new Combo("lightning_rod", "lightning", "gravity"),
		new Combo("thunder_charge", "lightning", "marked"), new Combo("storm_frenzy", "lightning", "rampaging"),
		new Combo("skybreaker", "lightning", "uppercut"), new Combo("toxic_storm", "lightning", "venomous"),
		new Combo("hexbolt", "lightning", "curse"), new Combo("gilded_storm", "lightning", "plunder"),
		new Combo("black_lightning", "lightning", "withering"), new Combo("shulker_storm", "lightning", "shulker_burst"),
		new Combo("arc", "lightning", "shared_pain"), new Combo("static_guard", "lightning", "guardian_strike"),
		new Combo("copper_storm", "lightning", "copper_shock"), new Combo("gale_echo", "wind_charged", "echo"),
		new Combo("whiplash", "wind_charged", "chains"), new Combo("hurricane", "wind_charged", "rampaging"),
		new Combo("toxic_gust", "wind_charged", "venomous"), new Combo("hexwind", "wind_charged", "curse"),
		new Combo("windfall", "wind_charged", "plunder"), new Combo("ashen_wind", "wind_charged", "withering"),
		new Combo("shulker_gust", "wind_charged", "shulker_burst"), new Combo("shrapnel", "wind_charged", "shared_pain"),
		new Combo("bulwark_gust", "wind_charged", "guardian_strike"), new Combo("spark_burst", "wind_charged", "copper_shock"));

	private final Lang lang;

	Scenarios(Lang lang) {
		this.lang = lang;
	}

	// ---- Text helpers ----

	/** The power whose Degrade was clicked once in the lab, showing Confirm. */
	private String confirming;

	private Rich p(String key, Object... args) {
		return lang.tr("panel.bcenchantments." + key, args);
	}

	private Rich g(String key, Object... args) {
		return lang.tr("guide.bcenchantments." + key, args);
	}

	private Rich desc(String key) {
		return Rich.keyed(lang.raw(key).replace('\n', ' '), key, List.of());
	}

	private Rich perkName(String id) {
		return lang.tr("enchantment.bcenchantments." + id);
	}

	private Rich numeral(int n) {
		return lang.tr("enchantment.level." + n);
	}

	private Rich tinted(String perk) {
		PerkInfo info = info(perk);
		return Rich.of(perkName(perk).plain(), trimColor(info.trim));
	}

	private static PerkInfo info(String id) {
		return PERKS.stream().filter(p -> p.id.equals(id)).findFirst().orElseThrow();
	}

	private static int trimColor(String trim) {
		return switch (trim) {
			case "bolt" -> 0xF2C94C;
			case "flow" -> 0x9ADCF0;
			case "tide" -> 0x4A8FD6;
			case "silence" -> 0x4FA6A0;
			case "ward" -> 0x7A6FD0;
			case "vex" -> 0x9FB0BE;
			case "eye" -> 0x6FD36B;
			case "wayfinder" -> 0xC8A07A;
			case "snout" -> 0xE0623C;
			case "raiser" -> 0xE8884A;
			default -> 0xF0F0F0;
		};
	}

	private static String trimName(String trim) {
		return trim.substring(0, 1).toUpperCase() + trim.substring(1);
	}

	private static ItemRef template(String trim) {
		return ItemRef.of(trim + "_armor_trim_smithing_template");
	}

	private Rich itemName(String id) {
		return Rich.of(lang.raw("item.minecraft." + id));
	}

	private Rows.Icon perkIcon(String perkId, Rich name, boolean highest, Rich sub) {
		PerkInfo info = info(perkId);
		return new Rows.Icon(template(info.trim), name, highest ? "max" : "bright", highest ? -1 : trimColor(info.trim), sub, "muted");
	}

	// ---- Smithing panel ----

	List<Smithing> smithing() {
		ItemRef sword = ItemRef.of("diamond_sword");
		ItemRef refined = ItemRef.of("netherite_sword").glinting();
		return List.of(
			new Smithing("Empty table", ItemRef.EMPTY, ItemRef.EMPTY, ItemRef.EMPTY, ItemRef.EMPTY, s -> List.of(
				new Rows.Text(p("empty"), "body"), new Rows.Space(6), new Rows.Text(p("empty.steps"), "muted"))),
			new Smithing("Refining a sword (level 3, paying)", ItemRef.EMPTY, sword, ItemRef.of("emerald", 32), sword.glinting(), s -> refining(sword)),
			new Smithing("Fully refined, trims to choose", template("bolt"), refined, ItemRef.of("echo_shard"), refined, this::trims),
			new Smithing("Fully refined, a power waits for a partner", ItemRef.EMPTY, refined, ItemRef.EMPTY, ItemRef.EMPTY, s -> waiting(refined)),
			new Smithing("Blocked upgrade (shows red)", template("eye"), refined, ItemRef.EMPTY, ItemRef.EMPTY, s -> blocked(refined)),
			new Smithing("Shield", template("flow"), ItemRef.of("shield"), ItemRef.of("echo_shard"), ItemRef.of("shield").glinting(), s -> shield()),
			new Smithing("Not refinable (iron sword)", ItemRef.EMPTY, ItemRef.of("iron_sword"), ItemRef.EMPTY, ItemRef.EMPTY, s -> List.of(
				new Rows.Header(ItemRef.of("iron_sword"), itemName("iron_sword"), Rich.EMPTY), new Rows.Text(p("why.tier"), "muted"))));
	}

	private Rows.Header header(ItemRef item, String id, Rich sub) {
		return new Rows.Header(item, itemName(id), sub);
	}

	private List<Row> stats(boolean changing) {
		List<Row> rows = new ArrayList<>();
		rows.add(new Rows.Heading(p("section.stats")));
		rows.add(new Rows.Stat(p("stat.damage"), Rich.of("8.4"), changing ? Rich.of("8.6") : null));
		rows.add(new Rows.Stat(p("stat.speed"), Rich.of("1.85"), changing ? Rich.of("1.9") : null));
		rows.add(new Rows.Stat(p("stat.crit_chance"), Rich.of("7.5%"), null));
		rows.add(new Rows.Stat(p("stat.crit_damage"), Rich.of("×1.55"), null));
		return rows;
	}

	private List<Row> refining(ItemRef sword) {
		List<Row> rows = new ArrayList<>();
		rows.add(header(sword, "diamond_sword", lang.tr("refinement.bcenchantments.level", numeral(2))));
		rows.addAll(stats(true));
		rows.add(new Rows.Heading(p("section.points")));
		rows.add(new Rows.Text(p("points.left", 2), "bright"));
		rows.add(new Rows.Space(2));
		rows.add(new Rows.PointRow(p("stat.damage"), Rich.of("+3%"), true, () -> { }));
		rows.add(new Rows.PointRow(p("stat.crit_chance"), Rich.of(""), true, () -> { }));
		rows.add(new Rows.PointRow(p("stat.crit_damage"), Rich.of("+3%"), true, () -> { }));
		rows.add(new Rows.Text(p("points.how"), "faint"));
		rows.add(new Rows.Heading(p("section.refinement")));
		rows.add(new Rows.Text(p("level_of", numeral(3), numeral(5)), "body"));
		rows.add(new Rows.Space(2));
		rows.add(new Rows.Bar(60, 32, 180));
		rows.add(new Rows.Text(p("gems", 92, 180), "muted"));
		return rows;
	}

	private List<Row> maxedHead(ItemRef refined, boolean spent) {
		List<Row> rows = new ArrayList<>();
		rows.add(header(refined, "netherite_sword", lang.tr("refinement.bcenchantments.level", numeral(5))));
		rows.addAll(stats(false));
		rows.add(new Rows.Heading(p("section.points")));
		rows.add(new Rows.Text(spent ? p("points.none") : p("points.left", 1), spent ? "muted" : "bright"));
		rows.add(new Rows.Space(2));
		rows.add(new Rows.PointRow(p("stat.damage"), Rich.of("+3%"), !spent, () -> { }));
		rows.add(new Rows.PointRow(p("stat.crit_chance"), Rich.of("+6%"), !spent, () -> { }));
		rows.add(new Rows.PointRow(p("stat.crit_damage"), Rich.of("+3%"), !spent, () -> { }));
		rows.add(new Rows.Heading(p("section.powers")));
		return rows;
	}

	private void power(List<Row> rows, String perk, int level) {
		boolean highest = level >= 3;
		Rich name = Rich.of(perkName(perk).plain() + " " + numeral(level).plain());
		rows.add(perkIcon(perk, name, highest, desc("tooltip.bcenchantments.power." + perk)));
		rows.add(new Rows.PowerButtons(p("downgrade"), p("degrade_confirm"), p("upgrade"), p("degrade_warning"), level > 1, !highest,
			perk.equals(confirming), () -> confirming = perk.equals(confirming) ? null : perk, () -> { }));
		rows.add(new Rows.Space(3));
	}

	private List<Row> trims(State s) {
		List<Row> rows = maxedHead(ItemRef.of("netherite_sword").glinting(), true);
		// What the table's inputs do: the preview sits under the header, its cost centred.
		int head = 1;
		rows.add(head++, new Rows.Text(p("preview.add", tinted("cold_edge")), "bright"));
		rows.add(head++, new Rows.Space(6));
		rows.add(head++, new Rows.Centered(p("preview.cost", 30), "bright"));
		rows.add(head, new Rows.Space(6));
		power(rows, "lightning", 1);
		power(rows, "wind_charged", 3);
		rows.add(new Rows.Title(p("section.pairs")));
		rows.add(new Rows.Space(8));
		rows.add(new Rows.Pair(p("pair", tinted("lightning"), tinted("wind_charged")), lang.tr("synergy.bcenchantments.charged_lightning")));
		rows.add(new Rows.Text(desc("tooltip.bcenchantments.synergy.charged_lightning"), "muted", 6));
		rows.add(new Rows.Space(5));
		rows.add(new Rows.Space(6));
		rows.add(new Rows.Centered(p("levels_missing", 10), "missing"));
		rows.add(new Rows.Text(p("how_trim"), "faint"));
		rows.add(new Rows.Heading(p("section.possible")));
		rows.add(new Rows.Space(2));
		for (String perk : List.of("cold_edge", "shockwave", "echo")) {
			PerkInfo info = info(perk);
			Rows.Icon icon = perkIcon(perk, perkName(perk), false, desc("tooltip.bcenchantments.power." + perk));
			boolean open = perk.equals(s.expanded);
			rows.add(new Rows.TrimRow(icon, open, () -> s.expanded = open ? null : perk));
			if (open) {
				rows.add(new Rows.Space(3));
				for (Combo combo : COMBOS) {
					String partner = combo.a.equals(perk) ? combo.b : combo.b != null && combo.b.equals(perk) ? combo.a : null;
					if (partner == null) {
						continue;
					}
					boolean free = !partner.equals("lightning") && !partner.equals("wind_charged");
					int indent = 22;
					rows.add(new Rows.Text(lang.tr("synergy.bcenchantments." + combo.id), free ? "bright" : "muted", indent));
					rows.add(new Rows.Text(p("combo_with", tinted(partner), trimName(info(partner).trim)), free ? "body" : "faint", indent));
					rows.add(new Rows.Text(desc("tooltip.bcenchantments.synergy." + combo.id), free ? "muted" : "faint", indent));
					rows.add(new Rows.Space(4));
				}
			}
			rows.add(new Rows.Space(8));
		}
		return rows;
	}

	private List<Row> waiting(ItemRef refined) {
		List<Row> rows = maxedHead(refined, false);
		power(rows, "lightning", 2);
		power(rows, "wind_charged", 1);
		power(rows, "chains", 3);
		rows.add(new Rows.Title(p("section.pairs")));
		rows.add(new Rows.Space(8));
		rows.add(new Rows.Pair(p("pair", tinted("lightning"), tinted("wind_charged")), lang.tr("synergy.bcenchantments.charged_lightning")));
		rows.add(new Rows.Text(desc("tooltip.bcenchantments.synergy.charged_lightning"), "muted", 6));
		rows.add(new Rows.Space(5));
		rows.add(new Rows.Text(p("alone", perkName("chains")), "faint"));
		rows.add(new Rows.Space(6));
		rows.add(new Rows.Centered(p("levels_enough"), "enough"));
		rows.add(new Rows.Text(p("how_trim"), "faint"));
		rows.add(new Rows.Heading(p("section.possible")));
		rows.add(new Rows.Text(p("waiting", perkName("chains")), "body"));
		rows.add(new Rows.Space(2));
		rows.add(new Rows.Icon(template("tide"), lang.tr("synergy.bcenchantments.frozen_chains"), "bright", -1,
			p("from", "Tide", perkName("cold_edge")), "muted"));
		rows.add(new Rows.Icon(template("eye"), lang.tr("synergy.bcenchantments.black_hole"), "muted", -1,
			p("from", "Eye", perkName("gravity")), "faint"));
		return rows;
	}

	private List<Row> blocked(ItemRef refined) {
		List<Row> rows = new ArrayList<>();
		rows.add(header(refined, "netherite_sword", lang.tr("refinement.bcenchantments.level", numeral(4))));
		rows.add(new Rows.Text(p("why.level"), "danger"));
		rows.addAll(stats(false));
		rows.add(new Rows.Heading(p("section.refinement")));
		rows.add(new Rows.Text(p("level_of", numeral(5), numeral(5)), "body"));
		rows.add(new Rows.Space(2));
		rows.add(new Rows.Bar(120, 0, 140));
		rows.add(new Rows.Text(p("gems", 120, 140), "muted"));
		rows.add(new Rows.Space(4));
		rows.add(new Rows.Text(p("unlocks"), "bright"));
		return rows;
	}

	private List<Row> shield() {
		List<Row> rows = new ArrayList<>();
		rows.add(header(ItemRef.of("shield"), "shield", p("shield")));
		rows.add(new Rows.Text(p("preview.add", tinted("wind_charged")), "bright"));
		rows.add(new Rows.Heading(p("section.powers")));
		rows.add(new Rows.Text(p("shield_none"), "body"));
		rows.add(new Rows.Space(2));
		for (String perk : List.of("cold_edge", "wind_charged")) {
			PerkInfo info = info(perk);
			boolean has = perk.equals("wind_charged");
			Rich title = Rich.of(perkName(perk).plain() + " · " + trimName(info.trim));
			rows.add(new Rows.Icon(template(info.trim), title, has ? "bright" : "muted", -1,
				desc("refinement.bcenchantments.shield." + perk), has ? "muted" : "faint"));
		}
		rows.add(new Rows.Text(p("how_trim"), "faint"));
		return rows;
	}

	// ---- Difficulty panel ----

	/** One inventory: the weapons on its hotbar and the rows the difficulty panel above it shows. */
	record Difficulty(String name, List<ItemRef> hotbar, Supplier<List<Row>> rows) {
	}

	/** {@code MobBalance.MAX_POWER} and {@code COUNTER_MAX_SHARE}, which the lab cannot reach. */
	private static final int MAX_POWER = 30;
	private static final int MAX_COUNTER = 20;

	List<Difficulty> difficulty() {
		ItemRef sword = ItemRef.of("diamond_sword").glinting();
		ItemRef axe = ItemRef.of("netherite_axe").glinting();
		ItemRef trident = ItemRef.of("trident").glinting();
		ItemRef mace = ItemRef.of("mace").glinting();
		return List.of(
			new Difficulty("Plains, no weapons", List.of(), () -> List.of(
				where(lang.tr("biome.minecraft.plains"), "regular", "1"),
				power("0"),
				new Rows.Text(d("no_weapons"), "muted"),
				new Rows.Text(d("not_adapted"), "muted"))),
			new Difficulty("One weapon, early adaptation", List.of(sword), () -> List.of(
				where(lang.tr("biome.minecraft.dripstone_caves"), "hard", "1.3"),
				power("6"),
				new Rows.Weapons(List.of(sword), List.of(d("weapon", "6"))),
				new Rows.Stat(d("adapted"), perkName("chains"), null),
				new Rows.Stat(d("countering"), d("countering.value", 3, MAX_COUNTER), null))),
			new Difficulty("Stronghold, Bad Omen III, four weapons", List.of(sword, axe, trident, mace), () -> List.of(
				where(lang.tr("landmark.bcenchantments.stronghold"), "possible_death", "1.8"),
				new Rows.Stat(d("omen"), Rich.of("III"), null),
				power("23.5"),
				new Rows.Weapons(List.of(sword, axe, trident, mace), List.of(d("weapon", "12"), d("weapon", "6"), d("weapon", "3.5"), d("weapon", "2"))),
				new Rows.Stat(d("adapted"), perkName("lightning"), null),
				new Rows.Stat(d("countering"), d("countering.value", 20, MAX_COUNTER), null))));
	}

	private Rich d(String key, Object... args) {
		return p("difficulty." + key, args);
	}

	private Rows.Stat where(Rich place, String tier, String multiplier) {
		return new Rows.Stat(place, d("tier", lang.tr("difficulty.bcenchantments." + tier), multiplier), null);
	}

	private Rows.Stat power(String value) {
		return new Rows.Stat(d("power"), d("power.value", value, MAX_POWER), null);
	}

	// ---- Field Guide ----

	List<Rich> tabLabels() {
		return List.of(g("tab.refining"), g("tab.perks"), g("tab.trims"), g("tab.combining"));
	}

	Rich guideTitle() {
		return g("title");
	}

	Rich guideHint() {
		return g("close");
	}

	List<Row> guide(int tab) {
		return switch (tab) {
			case 0 -> guideRefining();
			case 1 -> guidePerks();
			case 2 -> guideTrims();
			default -> guideCombining();
		};
	}

	private List<Row> guideRefining() {
		ItemRef sword = ItemRef.of("iron_sword");
		return List.of(
			new Rows.Paragraph(g("refining.intro")),
			new Rows.Recipes(List.of(
				new Rows.Recipe(ItemRef.EMPTY, sword, ItemRef.of("emerald", 32), sword.glinting(), g("refining.emeralds")),
				new Rows.Recipe(ItemRef.EMPTY, sword, ItemRef.of("emerald_block", 8), sword.glinting(), g("refining.blocks")))),
			new Rows.Paragraph(g("refining.levels")),
			new Rows.Paragraph(g("refining.points")),
			new Rows.Paragraph(g("refining.tooltip")));
	}

	private List<Row> guidePerks() {
		List<Row> rows = new ArrayList<>();
		rows.add(new Rows.Paragraph(g("perks.intro")));
		for (PerkInfo perk : PERKS) {
			if (perk.built) {
				rows.add(new Rows.Entry(List.of(template(perk.trim)), perkName(perk.id), perk.rgb,
					perk.charged ? g("perks.charged") : null, g("perk." + perk.id), List.of()));
			}
		}
		return rows;
	}

	private List<Row> guideTrims() {
		ItemRef sword = ItemRef.of("diamond_sword").glinting();
		ItemRef shard = ItemRef.of("echo_shard");
		List<Row> rows = new ArrayList<>();
		rows.add(new Rows.Recipe(template("bolt"), sword, shard, sword, g("trims.first")));
		rows.add(new Rows.Recipe(template("bolt"), sword, ItemRef.of("emerald", 64), sword, g("trims.raise")));
		rows.add(new Rows.Recipe(template("flow"), sword, shard, sword, g("trims.combine")));
		rows.add(new Rows.Paragraph(g("trims.which")));
		for (PerkInfo perk : PERKS) {
			rows.add(new Rows.Entry(List.of(template(perk.trim)), perkName(perk.id), perk.rgb, perk.built ? null : g("trims.coming"),
				Rich.of(trimName(perk.trim)), List.of()));
		}
		ItemRef shield = ItemRef.of("shield");
		rows.add(new Rows.Recipe(template("flow"), shield, shard, shield.glinting(), g("trims.shield")));
		rows.add(new Rows.Entry(List.of(shield.glinting()), perkName("cold_edge"), -1, g("trims.shield_power"), g("shield.cold_edge"),
			List.of(template("tide"))));
		rows.add(new Rows.Entry(List.of(shield.glinting()), perkName("wind_charged"), -1, g("trims.shield_power"), g("shield.wind_charged"),
			List.of(template("flow"))));
		return rows;
	}

	private List<Row> guideCombining() {
		List<Row> rows = new ArrayList<>();
		rows.add(new Rows.Paragraph(g("combining.intro")));
		for (Combo combo : COMBOS) {
			List<ItemRef> icons = new ArrayList<>();
			List<String> names = new ArrayList<>();
			for (String perk : combo.b == null ? List.of(combo.a) : List.of(combo.a, combo.b)) {
				icons.add(template(info(perk).trim));
				names.add(perkName(perk).plain());
			}
			if (combo.b == null) {
				icons.add(ItemRef.of("iron_sword").glinting());
				names.add(g("combining.refined").plain());
			}
			rows.add(new Rows.Entry(icons, lang.tr("synergy.bcenchantments." + combo.id), 0xFFAA00, Rich.of(String.join(" + ", names)),
				lang.tr("synergy.bcenchantments." + combo.id + ".desc"), List.of()));
		}
		return rows;
	}
}
