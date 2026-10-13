package dev.vadim.bcenchantments.mob;

/**
 * Every number behind the monster overhaul, in one place. The aim: more monsters, each one a little
 * softer per hit but sturdier, and dangerous when they keep the player pinned. Buffs stay small enough
 * that a zombie still reads as a zombie.
 */
final class MobBalance {
	// Spawning: more monsters at once, so encounters are crowds rather than stragglers.
	/** Multiplier on the monster spawn cap (vanilla 70 per player area). */
	static final float MONSTER_CAP_MULTIPLIER = 1.5F;

	// Pressure damage: a single hit is softened, but hits that keep coming hurt more and more.
	/** Share of its damage a monster's first hit in a chain deals. */
	static final float FIRST_HIT_DAMAGE = 0.75F;
	/** Extra damage for each earlier hit still in the chain, relative to the first hit's. */
	static final float CHAIN_STEP = 0.25F;
	/** Most a chained hit can deal, relative to the first hit's. */
	static final float CHAIN_CAP = 2.0F;
	/** A chain ends after this long without a monster hit: 2 seconds. */
	static final int CHAIN_RESET_TICKS = 40;

	// Hidden stats, given once when a monster joins the world. Power is the summed perk levels on the
	// strongest nearby player's held weapons; local difficulty runs 0 to 1 with the moon, the time the
	// chunk was lived in, and the world's difficulty.
	/** Players within this many blocks of a spawning monster count toward its power. */
	static final double POWER_RADIUS = 48.0;
	/** Power above this adds nothing more. */
	static final int MAX_POWER = 12;

	/** Max health added, as a share of the monster's own. */
	static final double HEALTH_BASE = 0.15;
	static final double HEALTH_PER_DIFFICULTY = 0.25;
	static final double HEALTH_PER_POWER = 0.05;

	/** Armor points added. */
	static final double ARMOR_BASE = 1.0;
	static final double ARMOR_PER_DIFFICULTY = 1.0;
	static final double ARMOR_PER_POWER = 0.25;

	/** Knockback resistance added, 0 to 1. */
	static final double KNOCKBACK_RESISTANCE_BASE = 0.05;
	static final double KNOCKBACK_RESISTANCE_PER_DIFFICULTY = 0.05;
	static final double KNOCKBACK_RESISTANCE_PER_POWER = 0.015;

	private MobBalance() {
	}
}
