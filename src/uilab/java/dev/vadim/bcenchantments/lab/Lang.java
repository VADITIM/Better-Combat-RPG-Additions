package dev.vadim.bcenchantments.lab;

import dev.vadim.bcenchantments.ui.Json;
import dev.vadim.bcenchantments.ui.Rich;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Vanilla's en_us lang from the jar plus the mod's own file. Edits to the mod's lines are written back to
 * its file line by line, so the rest of the file keeps its formatting.
 */
final class Lang {
	private final Map<String, String> vanilla = new LinkedHashMap<>();
	private final Map<String, String> mod = new LinkedHashMap<>();
	private final Set<String> dirty = new LinkedHashSet<>();
	private final Path file;

	Lang(Assets assets, Path file) throws IOException {
		this.file = file;
		String v = assets.text("lang/en_us.json");
		if (v != null) {
			Json.object(v).forEach((k, value) -> vanilla.put(k, String.valueOf(value)));
		}
		reload();
	}

	void reload() throws IOException {
		mod.clear();
		dirty.clear();
		Json.object(Files.readString(file, StandardCharsets.UTF_8)).forEach((k, value) -> mod.put(k, String.valueOf(value)));
	}

	boolean has(String key) {
		return mod.containsKey(key) || vanilla.containsKey(key);
	}

	boolean editable(String key) {
		return mod.containsKey(key);
	}

	/** The raw template for a key, with its placeholders; the key itself when unknown. */
	String raw(String key) {
		String value = mod.get(key);
		return value != null ? value : vanilla.getOrDefault(key, key);
	}

	void set(String key, String value) {
		if (mod.containsKey(key)) {
			mod.put(key, value);
			dirty.add(key);
		}
	}

	boolean isDirty() {
		return !dirty.isEmpty();
	}

	/** Text for {@code key} with {@code args} filled in; an argument that is a {@link Rich} keeps its colour. */
	Rich tr(String key, Object... args) {
		List<String> plain = new ArrayList<>();
		for (Object arg : args) {
			plain.add(arg instanceof Rich r ? r.plain() : String.valueOf(arg));
		}
		return new Rich(format(raw(key), args), key, plain);
	}

	/** A literal with no key. */
	static Rich lit(String text) {
		return Rich.of(text);
	}

	private static final Pattern PLACEHOLDER = Pattern.compile("%(?:(\\d+)\\$)?([sd%])");

	static List<Rich.Span> format(String template, Object[] args) {
		List<Rich.Span> spans = new ArrayList<>();
		Matcher m = PLACEHOLDER.matcher(template);
		int last = 0;
		int next = 0;
		StringBuilder pending = new StringBuilder();
		while (m.find()) {
			pending.append(template, last, m.start());
			last = m.end();
			if (m.group(2).equals("%")) {
				pending.append('%');
				continue;
			}
			int index = m.group(1) != null ? Integer.parseInt(m.group(1)) - 1 : next++;
			Object arg = index < args.length ? args[index] : "";
			if (arg instanceof Rich r) {
				if (!pending.isEmpty()) {
					spans.add(new Rich.Span(pending.toString(), -1));
					pending.setLength(0);
				}
				spans.addAll(r.spans());
			} else {
				pending.append(arg);
			}
		}
		pending.append(template.substring(last));
		if (!pending.isEmpty() || spans.isEmpty()) {
			spans.add(new Rich.Span(pending.toString(), -1));
		}
		return spans;
	}

	/** Writes every edited line back to the mod's lang file. Returns how many lines changed. */
	int save() throws IOException {
		String text = Files.readString(file, StandardCharsets.UTF_8);
		int changed = 0;
		for (String key : dirty) {
			Pattern value = Pattern.compile("(?m)^([ \\t]*" + Pattern.quote(Json.quote(key)) + "[ \\t]*:[ \\t]*)\"(?:[^\"\\\\]|\\\\.)*\"");
			Matcher m = value.matcher(text);
			if (m.find()) {
				text = m.replaceFirst(Matcher.quoteReplacement(m.group(1) + Json.quote(mod.get(key))));
				changed++;
			}
		}
		Files.writeString(file, text, StandardCharsets.UTF_8);
		dirty.clear();
		return changed;
	}
}
