package dev.vadim.bcenchantments.client;

/** Better Combat's client-side combo timer, exposed by {@code AttackInteractorMixin}. */
public final class ComboClock {
	public interface Source {
		/** Ticks since the last swing started. */
		int bcenchantments$lastAttacked();

		/** Ticks after which Better Combat restarts the combo. */
		int bcenchantments$comboReset();
	}

	public static Source source;

	private ComboClock() {
	}
}
