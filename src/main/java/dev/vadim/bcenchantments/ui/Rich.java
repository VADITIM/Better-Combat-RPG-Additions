package dev.vadim.bcenchantments.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * Text as coloured runs, plus the lang key and arguments it came from when it has them, so the UI lab can
 * show and change the wording a line uses. A colour of -1 means "the part's own colour".
 */
public record Rich(List<Span> spans, String key, List<String> args) {
	public record Span(String text, int rgb) {
	}

	public static final Rich EMPTY = new Rich(List.of(), null, List.of());

	public static Rich of(String text) {
		return new Rich(List.of(new Span(text, -1)), null, List.of());
	}

	public static Rich of(String text, int rgb) {
		return new Rich(List.of(new Span(text, rgb)), null, List.of());
	}

	public static Rich keyed(String text, String key, List<String> args) {
		return new Rich(List.of(new Span(text, -1)), key, args);
	}

	public Rich withSpans(List<Span> spans) {
		return new Rich(spans, key, args);
	}

	public String plain() {
		if (spans.size() == 1) {
			return spans.getFirst().text();
		}
		StringBuilder out = new StringBuilder();
		for (Span span : spans) {
			out.append(span.text());
		}
		return out.toString();
	}

	public boolean isEmpty() {
		return spans.isEmpty() || plain().isEmpty();
	}

	/** This text with {@code more} after it. */
	public Rich plus(Rich more) {
		List<Span> all = new ArrayList<>(spans);
		all.addAll(more.spans);
		return new Rich(all, null, List.of());
	}
}
