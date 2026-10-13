package dev.vadim.bcenchantments.combat;

/**
 * Turns Better Combat off: every weapon looks to it like it has no attributes, so attacks, sweeping
 * and reach are vanilla again, for farms built on vanilla sweeping. The client sets it from its
 * settings; in singleplayer and on a LAN host the integrated server shares it.
 */
public final class BetterCombatSwitch {
	private static volatile boolean disabled;

	private BetterCombatSwitch() {
	}

	public static boolean disabled() {
		return disabled;
	}

	public static void setDisabled(boolean value) {
		disabled = value;
	}
}
