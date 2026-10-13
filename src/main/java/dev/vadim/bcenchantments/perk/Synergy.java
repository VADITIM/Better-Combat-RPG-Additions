package dev.vadim.bcenchantments.perk;

import dev.vadim.bcenchantments.progression.Refinement;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Named combinations: perks on the same weapon that change each other. Each one is active when the
 * weapon has every perk it lists, at any level, and is fully refined if it asks for that; the perks
 * check for it where they behave differently.
 * <ul>
 * <li>Chained Lightning: Chains strikes the enemies it would bind with half-power lightning and roots them briefly.</li>
 * <li>Charged Lightning: Lightning bursts like a wind charge where it strikes.</li>
 * <li>Updraft: Uppercut sends the player and the target straight up, the player a block above, while the target hangs stunned; a mace then smashes as if from six blocks.</li>
 * <li>Frozen Chains: everything Chains binds is chilled.</li>
 * <li>Frost Burst: a death's wind burst chills what it catches.</li>
 * <li>Resonance: an Echo strike sparks a quarter-power lightning bolt on its target.</li>
 * <li>Tempest: a Shockwave throws enemies much harder, and one slammed into a wall takes knockback damage. A mace's smash also hurts the enemies within 2.5 blocks of the impact for 30% of its damage.</li>
 * <li>Frenzy: on a fully refined weapon, at its top attack speed, Echo strikes every third swing instead of every fourth.</li>
 * <li>Shatter: the weapon's lightning deals double damage to an enemy already frozen solid, and chills those around it; 15% more to one only slowed. Kills spread chill to the enemies around.</li>
 * <li>Singularity: a Shockwave drags the enemies beside its path into it.</li>
 * <li>Black Hole: every enemy Chains binds pulls the enemies around it in.</li>
 * <li>Cyclone: a kill draws the enemies around it in before its wind burst goes off.</li>
 * <li>Launch: a launching swing after a run-up throws the targets higher and the player up after them.</li>
 * <li>Blast Step: a kill by a swing after a run-up bursts the player onward.</li>
 * </ul>
 */
public enum Synergy {
	CHAINED_LIGHTNING("chained_lightning", Perks.CHAINS, Perks.LIGHTNING),
	CHARGED_LIGHTNING("charged_lightning", Perks.LIGHTNING, Perks.WIND_CHARGED),
	UPDRAFT("updraft", Perks.UPPERCUT, Perks.WIND_CHARGED),
	FROZEN_CHAINS("frozen_chains", Perks.CHAINS, Perks.COLD_EDGE),
	FROST_BURST("frost_burst", Perks.COLD_EDGE, Perks.WIND_CHARGED),
	RESONANCE("resonance", Perks.ECHO, Perks.LIGHTNING),
	TEMPEST("tempest", Perks.SHOCKWAVE, Perks.WIND_CHARGED),
	FRENZY("frenzy", true, Perks.ECHO),
	SHATTER("shatter", Perks.COLD_EDGE, Perks.LIGHTNING),
	SINGULARITY("singularity", Perks.GRAVITY, Perks.SHOCKWAVE),
	BLACK_HOLE("black_hole", Perks.GRAVITY, Perks.CHAINS),
	CYCLONE("cyclone", Perks.GRAVITY, Perks.WIND_CHARGED),
	LAUNCH("launch", Perks.MOMENTUM, Perks.UPPERCUT),
	BLAST_STEP("blast_step", Perks.MOMENTUM, Perks.WIND_CHARGED);

	private final String name;
	private final List<ResourceKey<Enchantment>> perks;
	private final boolean refined;

	@SafeVarargs
	Synergy(String name, ResourceKey<Enchantment>... perks) {
		this(name, false, perks);
	}

	/** {@code refined}: the weapon must also be at the last refinement level. */
	@SafeVarargs
	Synergy(String name, boolean refined, ResourceKey<Enchantment>... perks) {
		this.name = name;
		this.refined = refined;
		this.perks = List.of(perks);
	}

	public boolean on(ItemStack stack) {
		if (this.refined && Refinement.levelOf(stack) < Refinement.MAX_LEVEL) {
			return false;
		}
		for (ResourceKey<Enchantment> perk : this.perks) {
			if (Perks.level(stack, perk) <= 0) {
				return false;
			}
		}
		return !stack.isEmpty();
	}

	public String id() {
		return this.name;
	}

	public List<ResourceKey<Enchantment>> perks() {
		return this.perks;
	}

	/** Whether it also needs a fully refined weapon. */
	public boolean refined() {
		return this.refined;
	}

	public MutableComponent displayName() {
		return Component.translatable("synergy.bcenchantments." + this.name);
	}

	/** The weapon's active synergies as tooltip lines, placed under the item's name. */
	public static void tooltip(ItemStack stack, List<Component> lines) {
		if (lines.isEmpty()) {
			return;
		}
		List<Component> added = new ArrayList<>();
		for (Synergy synergy : values()) {
			if (synergy.on(stack)) {
				added.add(synergy.displayName().withStyle(ChatFormatting.GOLD));
			}
		}
		lines.addAll(1, added);
	}
}
