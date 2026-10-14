package dev.vadim.bcenchantments.client;

import dev.vadim.bcenchantments.ui.Canvas;
import dev.vadim.bcenchantments.ui.ItemRef;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** The shared UI code's {@link Canvas} over Minecraft's GUI drawing; bound to the frame's graphics before each use. */
final class GuiCanvas implements Canvas {
	private GuiGraphicsExtractor g;
	private Font font;

	void bind(GuiGraphicsExtractor g, Font font) {
		this.g = g;
		this.font = font;
	}

	@Override
	public void fill(int x0, int y0, int x1, int y1, int argb) {
		g.fill(x0, y0, x1, y1, argb);
	}

	@Override
	public void text(String text, int x, int y, int argb, boolean shadow) {
		g.text(font, text, x, y, argb, shadow);
	}

	@Override
	public int width(String text) {
		return font.width(text);
	}

	@Override
	public void item(ItemRef item, int x, int y) {
		ItemStack stack = item.payload() instanceof ItemStack s ? s : lookup(item.id());
		if (stack.isEmpty()) {
			return;
		}
		g.item(stack, x, y);
		if (stack.getCount() > 1) {
			g.itemDecorations(font, stack, x, y);
		}
	}

	private static ItemStack lookup(String id) {
		Identifier key = id.indexOf(':') >= 0 ? Identifier.parse(id) : Identifier.withDefaultNamespace(id);
		Item item = BuiltInRegistries.ITEM.getValue(key);
		return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
	}

	@Override
	public void push() {
		g.pose().pushMatrix();
	}

	@Override
	public void pop() {
		g.pose().popMatrix();
	}

	@Override
	public void translate(float x, float y) {
		g.pose().translate(x, y);
	}

	@Override
	public void scale(float factor) {
		g.pose().scale(factor, factor);
	}

	@Override
	public void scissor(int x0, int y0, int x1, int y1) {
		g.enableScissor(x0, y0, x1, y1);
	}

	@Override
	public void unscissor() {
		g.disableScissor();
	}
}
