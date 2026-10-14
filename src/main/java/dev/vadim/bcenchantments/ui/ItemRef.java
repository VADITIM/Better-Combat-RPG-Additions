package dev.vadim.bcenchantments.ui;

/**
 * An item to draw. {@code id} is the registry path ({@code "diamond_sword"}, namespace {@code minecraft}
 * unless it has a colon); {@code payload} is the game's own stack when there is one, so the mod draws it
 * exactly and shows its tooltip, while the lab draws from {@code id} alone.
 */
public record ItemRef(String id, int count, boolean glint, Object payload) {
	public static ItemRef of(String id) {
		return new ItemRef(id, 1, false, null);
	}

	public static ItemRef of(String id, int count) {
		return new ItemRef(id, count, false, null);
	}

	public ItemRef glinting() {
		return new ItemRef(id, count, true, payload);
	}

	public boolean empty() {
		return id == null || id.isEmpty() || id.equals("air");
	}

	public static final ItemRef EMPTY = new ItemRef("", 0, false, null);
}
