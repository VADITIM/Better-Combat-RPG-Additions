package dev.vadim.bcenchantments.ui;

/** One piece of a panel or guide tab, laid out top to bottom. See {@link Rows}. */
public interface Row {
	/** Names the row's parts in the sheet: {@code row.<kind>} holds its spacing. */
	String kind();

	int height(Ctx c, int width);

	void draw(Ctx c, int x, int y, int width);

	/** The text that tells this row apart from others of its kind, by its lang key; null if none does. See {@link Layout}. */
	default Rich label() {
		return null;
	}
}
