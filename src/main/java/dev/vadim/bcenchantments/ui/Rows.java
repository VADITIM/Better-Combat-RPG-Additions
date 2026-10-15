package dev.vadim.bcenchantments.ui;

import java.util.List;

/**
 * Every row the smithing panel and the Field Guide are made of. A row only says what it shows; where it
 * sits and how it looks comes from the {@link Sheet} by part id ({@code stat.label.color},
 * {@code header.slot.size}, ...), so the UI lab can change any of it. A tone names a text colour:
 * bright, body, muted, label, faint, danger, enough, missing or max.
 */
public final class Rows {
	private Rows() {
	}

	static String tone(String tone) {
		return switch (tone) {
			case "danger" -> "$DANGER";
			case "enough" -> "$ENOUGH";
			case "missing" -> "$MISSING";
			case "max" -> "$MAX_POWER";
			default -> "$TEXT_" + tone.toUpperCase(java.util.Locale.ROOT);
		};
	}

	static Ctx.Tx tx(String tone) {
		return Ctx.Tx.of(tone(tone));
	}

	private static final Ctx.Tx BRIGHT_SHADOW = Ctx.Tx.shadowed("$TEXT_BRIGHT");
	private static final Ctx.Fill RULE = new Ctx.Fill("$BORDER", 1.0F);
	private static final Ctx.Fill HIGHLIGHT = new Ctx.Fill("$HIGHLIGHT", 0.12F);
	private static final Ctx.Fill SELECTED = new Ctx.Fill("$HIGHLIGHT", 0.07F);
	private static final Ctx.BoxDef BUTTON = new Ctx.BoxDef("$SUNKEN", 0.85F, "$BORDER_CONTROL", "$SUNKEN_LIGHT", 0.9F, "$BORDER_HOVER");

	// ---- Smithing panel ----

	/** The item, its name and a line under it. */
	public record Header(ItemRef item, Rich name, Rich sub) implements Row {
		@Override
		public String kind() {
			return "header";
		}

		@Override
		public int height(Ctx c, int width) {
			return c.num("header.slot", "size", 18) + 2;
		}

		@Override
		public void draw(Ctx c, int x, int y, int width) {
			int size = c.slot("header.slot", item, x, y);
			int gap = c.num("header", "gap", 4);
			int tx = x + size + gap;
			c.text("header.name", c.ellipsize(name, width - size - gap), tx, y, BRIGHT_SHADOW);
			c.text("header.sub", sub, tx, y + c.line(), tx("label"));
		}
	}

	/** A section heading over a hairline. */
	public record Heading(Rich text) implements Row {
		@Override
		public String kind() {
			return "heading";
		}

		@Override
		public Rich label() {
			return text;
		}

		@Override
		public int height(Ctx c, int width) {
			return c.num("ui", "section_gap", 6) + c.line() + 2;
		}

		@Override
		public void draw(Ctx c, int x, int y, int width) {
			int gap = c.num("ui", "section_gap", 6);
			c.fill("heading.rule", x, y + gap / 2 - 1, x + width, y + gap / 2, RULE);
			c.text("heading.text", text, x, y + gap, tx("muted"));
		}
	}

	/** A wrapped paragraph in one of the text tones. */
	public record Text(Rich text, String tone, int indent) implements Row {
		public Text(Rich text, String tone) {
			this(text, tone, 0);
		}

		@Override
		public String kind() {
			return "text";
		}

		@Override
		public Rich label() {
			return text;
		}

		@Override
		public int height(Ctx c, int width) {
			return c.lineCount(text, width - indent) * c.line();
		}

		@Override
		public void draw(Ctx c, int x, int y, int width) {
			c.lines("text." + tone, text, x + indent, y, width - indent, tx(tone));
		}
	}

	/** A stat: its name on the left, its value on the right, and the value the table's inputs would give. */
	public record Stat(Rich label, Rich value, Rich next) implements Row {
		@Override
		public String kind() {
			return "stat";
		}

		@Override
		public int height(Ctx c, int width) {
			return c.line();
		}

		@Override
		public void draw(Ctx c, int x, int y, int width) {
			c.text("stat.label", label, x, y, tx("label"));
			int right = x + width;
			Rich arrow = Rich.of(" → ");
			if (next != null) {
				right -= c.width("stat.next", next, tx("bright"));
				c.text("stat.next", next, right, y, tx("bright"));
				right -= c.width("stat.arrow", arrow, tx("faint"));
				c.text("stat.arrow", arrow, right, y, tx("faint"));
			}
			String part = next != null ? "stat.value_changing" : "stat.value";
			Ctx.Tx style = tx(next != null ? "muted" : "body");
			c.text(part, value, right - c.width(part, value, style), y, style);
		}
	}

	/** Emeralds paid toward the next level as a bar: paid, about to be paid, still needed. */
	public record Bar(int paid, int incoming, int total) implements Row {
		@Override
		public String kind() {
			return "bar";
		}

		@Override
		public int height(Ctx c, int width) {
			return c.num("bar", "height", 6);
		}

		@Override
		public void draw(Ctx c, int x, int y, int width) {
			int h = height(c, width);
			int filled = total <= 0 ? 0 : Math.round(width * Math.max(0.0F, Math.min(1.0F, paid / (float) total)));
			int coming = total <= 0 ? 0 : Math.round(width * Math.max(0.0F, Math.min(1.0F, (paid + incoming) / (float) total)));
			c.fill("bar.track", x, y + 1, x + width, y + h - 1, new Ctx.Fill("$ACCENT", 0.25F));
			c.fill("bar.incoming", x + filled, y + 1, x + coming, y + h - 1, new Ctx.Fill("$EMERALD_SOFT", 1.0F));
			c.fill("bar.paid", x, y + 1, x + filled, y + h - 1, new Ctx.Fill("$EMERALD", 1.0F));
		}
	}

	/** An item icon with a title beside it and optional wrapped text under the title. */
	public record Icon(ItemRef item, Rich title, String titleTone, int titleRgb, Rich sub, String subTone) implements Row {
		@Override
		public String kind() {
			return "icon";
		}

		@Override
		public Rich label() {
			return title;
		}

		@Override
		public int height(Ctx c, int width) {
			int size = c.num("icon.slot", "size", 18);
			int text = c.line() + (sub == null ? 0 : c.lineCount(sub, width - size - c.num("icon", "gap", 4)) * c.line());
			return Math.max(size, text) + 1;
		}

		@Override
		public void draw(Ctx c, int x, int y, int width) {
			int size = c.slot("icon.slot", item, x, y);
			int gap = c.num("icon", "gap", 4);
			int tx = x + size + gap;
			String part = titleRgb >= 0 ? "icon.title.custom" : "icon.title." + titleTone;
			c.text(part, c.ellipsize(title, width - size - gap), tx, y + (sub == null ? 5 : 1), tx(titleTone), false, titleRgb);
			if (sub != null) {
				c.lines("icon.sub." + subTone, sub, tx, y + 1 + c.line(), width - size - gap, tx(subTone));
			}
		}
	}

	/** A trim the player carries; a click opens or closes the list of everything it combines into. */
	public record TrimRow(Icon icon, boolean expanded, Runnable toggle) implements Row {
		@Override
		public String kind() {
			return "trim";
		}

		@Override
		public Rich label() {
			return icon.title();
		}

		@Override
		public int height(Ctx c, int width) {
			return icon.height(c, width);
		}

		@Override
		public void draw(Ctx c, int x, int y, int width) {
			int h = icon.height(c, width);
			boolean over = c.over(x, y, x + width, y + h);
			if (over || expanded) {
				c.fill(over ? "trim.highlight" : "trim.selected", x - 2, y - 1, x + width + 2, y + h, over ? HIGHLIGHT : SELECTED);
			}
			icon.draw(c, x, y, width);
			Rich mark = Rich.of(expanded ? "-" : "+");
			Ctx.Tx style = tx("faint").hover("$TEXT_BRIGHT");
			c.text("trim.mark", mark, x + width - c.width("trim.mark", mark, style), y + 1, style, over, -1);
			c.hit(x - 2, y - 1, x + width + 2, y + h, toggle);
		}
	}

	/**
	 * A stat a level point can go to: its name, the points already on it, and a "+" while the weapon has
	 * one to spend.
	 */
	public record PointRow(Rich name, Rich value, boolean open, Runnable spend) implements Row {
		@Override
		public String kind() {
			return "point";
		}

		@Override
		public Rich label() {
			return name;
		}

		@Override
		public int height(Ctx c, int width) {
			return c.line() + 2;
		}

		@Override
		public void draw(Ctx c, int x, int y, int width) {
			int h = height(c, width);
			boolean over = open && c.mouseX >= x && c.mouseX < x + width && c.mouseY >= y - 1 && c.mouseY < y + h - 1;
			if (over) {
				c.fill("point.highlight", x - 2, y - 1, x + width + 2, y + h - 1, HIGHLIGHT);
			}
			c.text(open ? "point.name" : "point.name_closed", name, x, y + 1, tx(open ? "body" : "label"));
			Rich mark = Rich.of("+");
			int right = x + width;
			if (open) {
				Ctx.Tx style = tx("muted").hover("$TEXT_BRIGHT");
				right -= c.width("point.mark", mark, style);
				c.text("point.mark", mark, right, y + 1, style, over, -1);
				right -= c.num("point", "mark_gap", 6);
			}
			Ctx.Tx valueStyle = tx("muted");
			c.text("point.value", value, right - c.width("point.value", value, valueStyle), y + 1, valueStyle);
			if (open) {
				c.hit(x - 2, y - 1, x + width + 2, y + h - 1, spend);
			}
		}
	}

	/**
	 * "Degrade" and "Upgrade" side by side under a power. Upgrade puts the trim template and emeralds on the
	 * table. Degrade turns into a red {@code confirm} button ({@code confirming}); hovering either shows
	 * {@code warning}. Faint when it cannot be done.
	 */
	public record PowerButtons(Rich degrade, Rich confirm, Rich upgrade, Rich warning, boolean canDegrade, boolean canUpgrade,
		boolean confirming, Runnable onDegrade, Runnable onUpgrade) implements Row {
		@Override
		public String kind() {
			return "power_buttons";
		}

		@Override
		public int height(Ctx c, int width) {
			return c.line() + 4;
		}

		@Override
		public void draw(Ctx c, int x, int y, int width) {
			int h = height(c, width);
			int indent = c.num("icon.slot", "size", 18) + c.num("icon", "gap", 4);
			x += indent;
			width -= indent;
			int gap = c.num("power_buttons", "gap", 4);
			int half = (width - gap) / 2;
			boolean down = canDegrade || confirming;
			if (confirming) {
				button(c, x, y, half, h, "power_button.confirm", confirm, true, CONFIRM, "$DANGER", onDegrade);
			} else {
				button(c, x, y, half, h, "power_button", degrade, canDegrade, BUTTON, "$TEXT_BODY", onDegrade);
			}
			if (down && c.over(x, y, x + half, y + h - 1)) {
				c.tip = warning;
			}
			button(c, x + half + gap, y, half, h, "power_button", upgrade, canUpgrade, BUTTON, "$TEXT_BODY", onUpgrade);
		}

		private static final Ctx.BoxDef CONFIRM = new Ctx.BoxDef("$DANGER", 0.25F, "$DANGER", "$DANGER", 0.4F, "$DANGER");

		private void button(Ctx c, int x, int y, int w, int h, String id, Rich label, boolean usable, Ctx.BoxDef look, String color, Runnable action) {
			boolean over = usable && c.mouseX >= x && c.mouseX < x + w && c.mouseY >= y && c.mouseY < y + h - 1;
			c.box(id + ".box", look, over, x, y, x + w, y + h - 1);
			Ctx.Tx style = new Ctx.Tx(usable ? color : "$TEXT_FAINT", false, 1.0F, color.equals("$TEXT_BODY") ? "$TEXT_BRIGHT" : color);
			String part = usable ? id + ".label" : id + ".label_disabled";
			c.text(part, label, x + (w - c.width(part, label, style)) / 2, y + 3, style, over, -1);
			if (usable) {
				c.hit(x, y, x + w, y + h - 1, action);
			}
		}
	}

	/** A line of text centred in the panel. */
	public record Centered(Rich text, String tone) implements Row {
		@Override
		public String kind() {
			return "centered";
		}

		@Override
		public Rich label() {
			return text;
		}

		@Override
		public int height(Ctx c, int width) {
			return c.line() + 2;
		}

		@Override
		public void draw(Ctx c, int x, int y, int width) {
			Ctx.Tx style = tx(tone);
			String part = "centered." + tone;
			c.text(part, text, x + (width - c.width(part, text, style)) / 2, y + 1, style);
		}
	}

	/** A section title centred on its row at twice the font size, over a hairline. */
	public record Title(Rich text) implements Row {
		private static final Ctx.Tx STYLE = Ctx.Tx.of("$TEXT_BODY").scaled(2.0F);

		@Override
		public String kind() {
			return "title";
		}

		@Override
		public Rich label() {
			return text;
		}

		@Override
		public int height(Ctx c, int width) {
			return c.num("ui", "section_gap", 6) + Math.round(c.sheet.flt("title.text.scale", 2.0F) * c.line()) + 2;
		}

		@Override
		public void draw(Ctx c, int x, int y, int width) {
			int gap = c.num("ui", "section_gap", 6);
			c.fill("title.rule", x, y + gap / 2 - 1, x + width, y + gap / 2, RULE);
			int w = c.width("title.text", text, STYLE);
			c.text("title.text", text, x + (width - w) / 2, y + gap + 1, STYLE);
		}
	}

	/** A combination: its two powers in their trims' colours, and the result centred under them. */
	public record Pair(Rich powers, Rich result) implements Row {
		@Override
		public String kind() {
			return "pair";
		}

		@Override
		public Rich label() {
			return result;
		}

		@Override
		public int height(Ctx c, int width) {
			return 2 * c.line();
		}

		@Override
		public void draw(Ctx c, int x, int y, int width) {
			c.text("pair.powers", powers, x, y, tx("body"));
			Ctx.Tx style = tx("bright");
			c.text("pair.result", result, x + (width - c.width("pair.result", result, style)) / 2, y + c.line(), style);
		}
	}

	public record Space(int size) implements Row {
		@Override
		public String kind() {
			return "space";
		}

		@Override
		public int height(Ctx c, int width) {
			return size;
		}

		@Override
		public void draw(Ctx c, int x, int y, int width) {
		}
	}

	// ---- Difficulty panel ----

	/** Weapons side by side, each with what it adds beside it, as many as fit the width. */
	public record Weapons(List<ItemRef> items, List<Rich> values) implements Row {
		@Override
		public String kind() {
			return "weapons";
		}

		@Override
		public int height(Ctx c, int width) {
			return c.num("weapons.slot", "size", 18) + 1;
		}

		@Override
		public void draw(Ctx c, int x, int y, int width) {
			int gap = c.num("weapons", "gap", 6);
			int textGap = c.num("weapons", "text_gap", 2);
			Ctx.Tx style = tx("body");
			int cx = x;
			for (int i = 0; i < items.size(); i++) {
				Rich value = values.get(i);
				int size = c.num("weapons.slot", "size", 18);
				int need = size + textGap + c.width("weapons.value", value, style);
				if (cx + need > x + width) {
					break;
				}
				c.slot("weapons.slot", items.get(i), cx, y);
				c.text("weapons.value", value, cx + size + textGap, y + (size - c.line()) / 2 + 1, style);
				cx += need + gap;
			}
		}
	}

	// ---- Field Guide ----

	/** A wrapped paragraph in body text. */
	public record Paragraph(Rich text) implements Row {
		@Override
		public String kind() {
			return "paragraph";
		}

		@Override
		public Rich label() {
			return text;
		}

		@Override
		public int height(Ctx c, int width) {
			return c.lineCount(text, width) * c.line();
		}

		@Override
		public void draw(Ctx c, int x, int y, int width) {
			c.lines("paragraph.text", text, x, y, width, tx("body"));
		}
	}

	/** A smithing table recipe, slot by slot as the table lays it out, with a caption under it. */
	public record Recipe(ItemRef template, ItemRef base, ItemRef addition, ItemRef result, Rich caption) implements Row {
		@Override
		public String kind() {
			return "recipe";
		}

		@Override
		public Rich label() {
			return caption;
		}

		@Override
		public int height(Ctx c, int width) {
			return c.num("recipe.slot", "size", 18) + c.num("recipe", "caption_gap", 6) + c.lineCount(caption, width) * c.line();
		}

		@Override
		public void draw(Ctx c, int x, int y, int width) {
			int sx = x;
			ItemRef[] inputs = {template, base, addition};
			int size = c.num("recipe.slot", "size", 18);
			for (ItemRef input : inputs) {
				c.slot("recipe.slot", input, sx, y);
				sx += size + c.num("recipe", "gap", 2);
			}
			sx += c.num("recipe", "arrow_before", 4);
			Rich arrow = Rich.of("→");
			Ctx.Tx style = tx("muted");
			c.text("recipe.arrow", arrow, sx, y + 5, style);
			sx += c.width("recipe.arrow", arrow, style) + c.num("recipe", "arrow_after", 6);
			c.slot("recipe.slot", result, sx, y);
			c.lines("recipe.caption", caption, x, y + size + c.num("recipe", "caption_gap", 6), width, tx("muted"));
		}

		/** The width of the slots and arrow, without the caption. */
		public int slotsWidth(Ctx c) {
			int size = c.num("recipe.slot", "size", 18);
			Rich arrow = Rich.of("→");
			return 3 * (size + c.num("recipe", "gap", 2)) + c.num("recipe", "arrow_before", 4)
				+ c.width("recipe.arrow", arrow, tx("muted")) + c.num("recipe", "arrow_after", 6) + size;
		}
	}

	/** Recipes side by side, centred as a group with a gap between them; each caption wraps under its own recipe. */
	public record Recipes(List<Recipe> recipes) implements Row {
		@Override
		public String kind() {
			return "recipes";
		}

		@Override
		public Rich label() {
			return recipes.isEmpty() ? Rich.EMPTY : recipes.getFirst().caption();
		}

		@Override
		public int height(Ctx c, int width) {
			int height = 0;
			for (Recipe recipe : recipes) {
				height = Math.max(height, recipe.height(c, recipe.slotsWidth(c)));
			}
			return height;
		}

		@Override
		public void draw(Ctx c, int x, int y, int width) {
			int gap = c.num("recipes", "gap", 24);
			int total = -gap;
			for (Recipe recipe : recipes) {
				total += recipe.slotsWidth(c) + gap;
			}
			int rx = x + Math.max(0, (width - total) / 2);
			for (Recipe recipe : recipes) {
				int w = recipe.slotsWidth(c);
				recipe.draw(c, rx, y, w);
				rx += w + gap;
			}
		}
	}

	/**
	 * Icons on the left, a coloured title with an optional subtitle beside them, a description under the
	 * title and an optional row of extra icons under that.
	 */
	public record Entry(List<ItemRef> icons, Rich title, int titleRgb, Rich subtitle, Rich body, List<ItemRef> row) implements Row {
		private int textX(Ctx c) {
			return icons.size() * (c.num("entry.slot", "size", 18) + c.num("entry", "gap", 2)) + c.num("entry", "text_gap", 4);
		}

		@Override
		public String kind() {
			return "entry";
		}

		@Override
		public Rich label() {
			return title;
		}

		@Override
		public int height(Ctx c, int width) {
			int line = c.line();
			int size = c.num("entry.slot", "size", 18);
			int text = line + (subtitle == null ? 0 : line) + c.lineCount(body, width - textX(c)) * line;
			if (!row.isEmpty()) {
				text += 2 + size;
			}
			return Math.max(size, text);
		}

		@Override
		public void draw(Ctx c, int x, int y, int width) {
			int size = c.num("entry.slot", "size", 18);
			int step = size + c.num("entry", "gap", 2);
			for (int i = 0; i < icons.size(); i++) {
				c.slot("entry.slot", icons.get(i), x + i * step, y);
			}
			int tx = x + textX(c);
			int ty = y + 1;
			c.text(titleRgb >= 0 ? "entry.title.custom" : "entry.title", title, tx, ty, BRIGHT_SHADOW, false, titleRgb);
			ty += c.line();
			if (subtitle != null) {
				c.text("entry.subtitle", subtitle, tx, ty, tx("label"));
				ty += c.line();
			}
			ty += c.lines("entry.body", body, tx, ty, width - textX(c), tx("body"));
			if (!row.isEmpty()) {
				ty += 2;
				for (int i = 0; i < row.size(); i++) {
					c.slot("entry.row_slot", row.get(i), tx + i * (c.num("entry.row_slot", "size", 18) + c.num("entry", "gap", 2)), ty);
				}
			}
		}
	}
}
