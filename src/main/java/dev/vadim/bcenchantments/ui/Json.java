package dev.vadim.bcenchantments.ui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A small JSON reader and flat-object writer, so the shared UI code needs no library. */
public final class Json {
	private final String src;
	private int pos;

	private Json(String src) {
		this.src = src;
	}

	/** Objects become {@code LinkedHashMap}, arrays {@code ArrayList}, numbers {@code Double}. */
	public static Object parse(String text) {
		if (text.startsWith("﻿")) {
			text = text.substring(1);
		}
		Json json = new Json(text);
		json.ws();
		Object value = json.value();
		json.ws();
		if (json.pos != text.length()) {
			throw json.fail("trailing data");
		}
		return value;
	}

	@SuppressWarnings("unchecked")
	public static Map<String, Object> object(String text) {
		return (Map<String, Object>) parse(text);
	}

	/** A JSON string literal, quoted. */
	public static String quote(String s) {
		StringBuilder out = new StringBuilder("\"");
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			switch (c) {
				case '"' -> out.append("\\\"");
				case '\\' -> out.append("\\\\");
				case '\n' -> out.append("\\n");
				case '\r' -> out.append("\\r");
				case '\t' -> out.append("\\t");
				default -> {
					if (c < 0x20) {
						out.append(String.format("\\u%04x", (int) c));
					} else {
						out.append(c);
					}
				}
			}
		}
		return out.append('"').toString();
	}

	private IllegalArgumentException fail(String why) {
		return new IllegalArgumentException("JSON " + why + " at " + pos);
	}

	private void ws() {
		while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) {
			pos++;
		}
	}

	private Object value() {
		if (pos >= src.length()) {
			throw fail("ended early");
		}
		char c = src.charAt(pos);
		if (c == '{') {
			pos++;
			Map<String, Object> map = new LinkedHashMap<>();
			ws();
			if (src.charAt(pos) == '}') {
				pos++;
				return map;
			}
			while (true) {
				ws();
				String key = string();
				ws();
				expect(':');
				ws();
				map.put(key, value());
				ws();
				if (src.charAt(pos) == ',') {
					pos++;
					continue;
				}
				expect('}');
				return map;
			}
		}
		if (c == '[') {
			pos++;
			List<Object> list = new ArrayList<>();
			ws();
			if (src.charAt(pos) == ']') {
				pos++;
				return list;
			}
			while (true) {
				ws();
				list.add(value());
				ws();
				if (src.charAt(pos) == ',') {
					pos++;
					continue;
				}
				expect(']');
				return list;
			}
		}
		if (c == '"') {
			return string();
		}
		if (src.startsWith("true", pos)) {
			pos += 4;
			return Boolean.TRUE;
		}
		if (src.startsWith("false", pos)) {
			pos += 5;
			return Boolean.FALSE;
		}
		if (src.startsWith("null", pos)) {
			pos += 4;
			return null;
		}
		int start = pos;
		while (pos < src.length() && "+-0123456789.eE".indexOf(src.charAt(pos)) >= 0) {
			pos++;
		}
		if (start == pos) {
			throw fail("unexpected '" + c + "'");
		}
		return Double.valueOf(src.substring(start, pos));
	}

	private void expect(char c) {
		if (pos >= src.length() || src.charAt(pos) != c) {
			throw fail("expected '" + c + "'");
		}
		pos++;
	}

	private String string() {
		expect('"');
		StringBuilder out = new StringBuilder();
		while (true) {
			if (pos >= src.length()) {
				throw fail("unterminated string");
			}
			char c = src.charAt(pos++);
			if (c == '"') {
				return out.toString();
			}
			if (c != '\\') {
				out.append(c);
				continue;
			}
			char e = src.charAt(pos++);
			switch (e) {
				case 'n' -> out.append('\n');
				case 't' -> out.append('\t');
				case 'r' -> out.append('\r');
				case 'b' -> out.append('\b');
				case 'f' -> out.append('\f');
				case 'u' -> {
					out.append((char) Integer.parseInt(src.substring(pos, pos + 4), 16));
					pos += 4;
				}
				default -> out.append(e);
			}
		}
	}
}
