package dev.vadim.bcenchantments.client;

import dev.vadim.bcenchantments.ui.Sheet;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The look of the Field Guide and the smithing panel, read once from {@code assets/bcenchantments/ui/style.json}.
 * The UI lab ({@code ./gradlew uiLab}) edits that file; the next build ships the change.
 */
public final class UiSheet {
	private static final Logger LOGGER = LoggerFactory.getLogger("bcenchantments");
	private static Sheet sheet;

	private UiSheet() {
	}

	public static Sheet get() {
		if (sheet == null) {
			sheet = load();
		}
		return sheet;
	}

	private static Sheet load() {
		try (InputStream in = UiSheet.class.getResourceAsStream("/assets/bcenchantments/ui/style.json")) {
			if (in != null) {
				return Sheet.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
			}
		} catch (IOException | RuntimeException e) {
			LOGGER.error("Could not read the UI style sheet, using defaults", e);
		}
		return new Sheet();
	}
}
