package dev.vadim.bcenchantments.mob;

/**
 * Every number behind the monster overhaul, in one place. The aim: more monsters, each one a little
 * softer per hit but sturdier, and dangerous when they keep the player pinned. Buffs stay small enough
 * that a zombie still reads as a zombie.
 */
final class MobBalance {
	// Spawning: more monsters at once, so encounters are crowds rather than stragglers.
	/** Multiplier on the monster spawn cap (vanilla 70 per player area). */
	static final float MONSTER_CAP_MULTIPLIER = 1.8F;

	// Pressure damage: a single hit is softened, but hits that keep coming hurt more and more.
	/** Share of its damage a monster's first hit in a chain deals. */
	static final float FIRST_HIT_DAMAGE = 0.75F;
	/** Extra damage for each earlier hit still in the chain, relative to the first hit's. */
	static final float CHAIN_STEP = 0.25F;
	/** Most a chained hit can deal, relative to the first hit's. */
	static final float CHAIN_CAP = 2.0F;
	/** A chain ends after this long without a monster hit: 2 seconds. */
	static final int CHAIN_RESET_TICKS = 40;

	// Hidden stats, given once when a monster joins the world. Power is the strongest nearby player's
	// weapon power ({@link Adaptation}); local difficulty runs 0 to 1 with the moon, the time the
	// chunk was lived in, and the world's difficulty.
	/** Players within this many blocks of a spawning monster count toward its power. */
	static final double POWER_RADIUS = 48.0;
	/** Power above this adds nothing more. */
	static final int MAX_POWER = 30;
	/** Each power pair on a weapon counts this much power on top of the power levels. */
	static final int PAIR_POWER = 2;
	/**
	 * Every weapon the player carries counts, strongest first, each this share of the one before: the
	 * strongest in full, the second half, the third a quarter. More weapons add less and less.
	 */
	static final double WEAPON_FALLOFF = 0.5;

	// Adaptation: monsters learn the trim power a player leans on. Each weapon counts how often each of
	// its powers fires; the power furthest toward its count below is the one monsters adapt to, and
	// that share of new monsters near the player spawn countering it, up to COUNTER_MAX_SHARE.
	/** Most of the monsters spawning near a player that counter their power. */
	static final double COUNTER_MAX_SHARE = 0.20;
	/** Fires to full adaptation for powers that go off on nearly every swing: Cold Edge, Gravity. */
	static final int ADAPT_EVERY_SWING = 1500;
	/** For powers with a condition: Momentum, Uppercut, Wind Charged, Rampaging. */
	static final int ADAPT_CONDITIONAL = 500;
	/** For charged powers, which fire once per long streak: Lightning, Shockwave, Chains, Echo. */
	static final int ADAPT_CHARGED = 150;
	/** Share of a countered power's damage that still lands on a monster countering it. */
	static final float COUNTER_DAMAGE = 0.25F;

	/**
	 * Most max health added, as a share of the monster's own. Each monster rolls between none and this,
	 * middling bonuses common and the extremes rare.
	 */
	static final double HEALTH_MAX = 0.30;
	/** Most attack damage added, as a share of the monster's own, rolled evenly. */
	static final double DAMAGE_MAX = 0.15;

	/** Armor points added. */
	static final double ARMOR_BASE = 1.0;
	static final double ARMOR_PER_DIFFICULTY = 1.0;
	static final double ARMOR_PER_POWER = 0.15;

	/** Knockback resistance added, 0 to 1. */
	static final double KNOCKBACK_RESISTANCE_BASE = 0.05;
	static final double KNOCKBACK_RESISTANCE_PER_DIFFICULTY = 0.05;
	static final double KNOCKBACK_RESISTANCE_PER_POWER = 0.01;

	// Bad Omen from an ominous bottle: opt-in difficulty. Monsters spawning near a player with it get
	// these on top, per omen level (1 to 5), and the player's kills may drop loot.
	static final int OMEN_MAX = 5;
	/** Max health added per omen level, as a share of the monster's own: +30% at V. */
	static final double OMEN_HEALTH = 0.06;
	/** Attack damage added per omen level, as a share of the monster's own: +15% at V. */
	static final double OMEN_DAMAGE = 0.03;
	/** Armor points added per omen level. */
	static final double OMEN_ARMOR = 0.5;
	/** Knockback resistance added per omen level. */
	static final double OMEN_KNOCKBACK_RESISTANCE = 0.02;
	/** Emerald drop chance on a kill: 5% at I, one point more per level, 9% at V. */
	static final float OMEN_EMERALD_BASE = 0.04F;
	static final float OMEN_EMERALD_PER_LEVEL = 0.01F;
	/** Echo shard drop chance on a kill at Bad Omen V. */
	static final float OMEN_ECHO_SHARD = 0.002F;

	// Place: every bonus above (health, damage, armor, knockback resistance, elite odds) is multiplied
	// by where the monster spawns.
	static final double PLACE_NEAR_SPAWN = 0.5;
	static final double PLACE_UNDERGROUND = 1.25;
	static final double PLACE_DEEP_DARK = 1.5;
	static final double PLACE_NETHER = 1.5;
	/** Soul Sand Valley and Basalt Deltas. */
	static final double PLACE_NETHER_HARSH = 1.65;
	static final double PLACE_END = 1.75;
	/** Overworld surface within this many blocks of world spawn counts as near spawn. */
	static final double SPAWN_RADIUS = 256.0;

	// Landmarks: inside a structure's pieces the place multiplier and tier are the structure's, unless
	// the surroundings are harder. Each needs gear a step up from the last. Villages and trial chambers
	// have none: villages stay a refuge, trial chambers scale with ominous bottles.
	static final double LANDMARK_RUINS = 1.15;
	static final double LANDMARK_TEMPLE = 1.25;
	static final double LANDMARK_MINESHAFT = 1.4;
	static final double LANDMARK_OUTPOST = 1.5;
	static final double LANDMARK_MONUMENT = 1.6;
	static final double LANDMARK_STRONGHOLD = 1.75;
	static final double LANDMARK_FORTRESS = 1.8;
	static final double LANDMARK_MANSION = 1.9;
	static final double LANDMARK_BASTION = 2.0;
	static final double LANDMARK_ANCIENT_CITY = 2.0;
	static final double LANDMARK_END_CITY = 2.1;
	/** How often, in ticks, the server tells each player which landmark they stand in, for the difficulty panel. */
	static final int LANDMARK_SYNC_TICKS = 10;

	// Armor, by the spot's tier (Place.Tier: Regular, Hard, Very Hard, Extreme, Possible Death; Bad
	// Omen raises it). Only the bcenchantments:armored_monsters tag wears it.
	/** Chance a monster spawns in armor at all. */
	static final double[] ARMOR_WEAR_CHANCE = {0.10, 0.22, 0.35, 0.50, 0.65};
	/** The range of materials: 0 copper, 1 chainmail, 2 iron, 3 diamond, 4 netherite; the better ones rarer. */
	static final int[] ARMOR_MATERIAL_MIN = {0, 0, 1, 2, 3};
	static final int[] ARMOR_MATERIAL_MAX = {1, 2, 3, 4, 4};
	/** The helmet always comes; each other piece comes with this chance. */
	static final double[] ARMOR_PIECE_CHANCE = {0.35, 0.45, 0.55, 0.65, 0.8};
	/** Chance each piece is enchanted, and at which enchanting level. */
	static final double[] ARMOR_ENCHANT_CHANCE = {0.0, 0.15, 0.35, 0.6, 0.85};
	static final int[] ARMOR_ENCHANT_LEVEL = {0, 8, 15, 22, 30};
	/** Chance each worn piece drops on death; vanilla's is 8.5%. */
	static final float ARMOR_DROP_CHANCE = 0.04F;

	// Elites and captains. An elite carries one affix; a captain is an elite that rallies others.
	/** Chance a monster spawns elite, before place. */
	static final double ELITE_CHANCE = 0.03;
	static final double ELITE_PER_OMEN = 0.01;
	static final double ELITE_PER_POWER = 0.001;
	/** Captain odds as a share of the elite odds; only zombies, skeletons and piglins can lead. */
	static final double CAPTAIN_SHARE = 0.35;
	/** Max health added for the rank, as a share of the monster's own. */
	static final double ELITE_HEALTH = 0.2;
	static final double CAPTAIN_HEALTH = 0.4;
	/** Elites show their affix and captains rally this often: 1 s. */
	static final int ELITE_PULSE_TICKS = 20;
	/** Monsters within this many blocks of a captain get Speed I and Resistance I. */
	static final double CAPTAIN_RADIUS = 8.0;
	static final int CAPTAIN_EFFECT_TICKS = 40;
	/** Frost: Slowness II for 3 s. Weakening: Weakness I for 5 s. */
	static final int FROST_TICKS = 60;
	static final int WEAKENING_TICKS = 100;
	/** Shredding: each hit strips this much armor, stacking, until 5 s pass without one. */
	static final double SHRED_ARMOR = 2.0;
	static final int SHRED_MAX_STACKS = 3;
	static final int SHRED_TICKS = 100;
	/** Emerald drop chance when a player kills one. */
	static final double ELITE_EMERALD = 0.2;
	static final double CAPTAIN_EMERALD = 0.5;

	private MobBalance() {
	}
}
