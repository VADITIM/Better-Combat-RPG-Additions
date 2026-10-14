package dev.vadim.bcenchantments.ui;

/**
 * What the shared UI code draws with. The mod implements it over {@code GuiGraphicsExtractor}; the UI lab
 * implements it with Java2D and Minecraft's own font and textures. Colours are 0xAARRGGBB and every
 * coordinate is in GUI pixels.
 */
public interface Canvas {
	void fill(int x0, int y0, int x1, int y1, int argb);

	/** One run of text in Minecraft's font at its own size. */
	void text(String text, int x, int y, int argb, boolean shadow);

	int width(String text);

	/** An item icon, 16 by 16 with its top left at ({@code x}, {@code y}), with its count and glint. */
	void item(ItemRef item, int x, int y);

	void push();

	void pop();

	void translate(float x, float y);

	void scale(float factor);

	void scissor(int x0, int y0, int x1, int y1);

	void unscissor();
}
