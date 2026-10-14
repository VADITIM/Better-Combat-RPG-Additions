package dev.vadim.bcenchantments.client;

import dev.vadim.bcenchantments.ui.ItemRef;
import dev.vadim.bcenchantments.ui.Rich;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;

/** Turns the game's text and items into what the shared UI code draws. */
final class UiText {
	private UiText() {
	}

	/** A component as coloured runs, remembering its lang key when it is a translation. */
	static Rich rich(Component component) {
		List<Rich.Span> spans = new ArrayList<>();
		component.<Boolean>visit((Style style, String text) -> {
			if (!text.isEmpty()) {
				TextColor color = style.getColor();
				spans.add(new Rich.Span(text, color == null ? -1 : color.getValue()));
			}
			return Optional.empty();
		}, Style.EMPTY);
		String key = null;
		List<String> args = List.of();
		if (component.getContents() instanceof TranslatableContents translatable) {
			key = translatable.getKey();
			List<String> out = new ArrayList<>();
			for (Object arg : translatable.getArgs()) {
				out.add(arg instanceof Component c ? c.getString() : String.valueOf(arg));
			}
			args = out;
		}
		return new Rich(spans, key, args);
	}

	static ItemRef ref(ItemStack stack) {
		if (stack.isEmpty()) {
			return ItemRef.EMPTY;
		}
		return new ItemRef(BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath(), stack.getCount(), stack.hasFoil(), stack);
	}
}
