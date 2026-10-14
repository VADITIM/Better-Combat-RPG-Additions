package dev.vadim.bcenchantments.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.vadim.bcenchantments.TrimEnchantments;
import dev.vadim.bcenchantments.combat.CriticalHits;
import dev.vadim.bcenchantments.perk.Perks;
import dev.vadim.bcenchantments.perk.Synergy;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import org.jspecify.annotations.Nullable;

/**
 * Weapon refinement at the smithing table. Gems (emeralds, or blocks of emerald worth nine) are paid
 * into a weapon until it reaches the next refinement level; each level adds attack speed and one
 * small, random stat boost and a level point the player spends on a stat of their choice
 * ({@link Points}), and rolls what the next level will cost. At level {@link #MAX_LEVEL} the
 * weapon is locked and takes trims: each trim template with an echo shard grants that trim's power.
 * The first is free; every further one costs experience levels. Another template of a trim the
 * weapon already has does nothing; a power goes up to {@link #MAX_PERK} for {@link #UPGRADE_COST}
 * emeralds from the smithing panel, and back down for a refund of the same.
 * <p>
 * Powers combine in the order they were applied: the first with the second, the third with the
 * fourth, and so on. A power already in a combination is locked out of every other, so a third
 * power stays alone until a fourth joins it. A shield skips the levels: a trim of one of the few
 * shield powers and an echo shard grant it once.
 *
 * @param level refinement level, 0 to {@link #MAX_LEVEL}
 * @param gems gems paid toward the next level
 * @param cost gems the next level costs, rolled when the previous one was reached
 * @param powers the trim powers, in the order they were applied
 * @param points the level points spent; every level earns one (see {@link Points})
 */
public record Refinement(int level, int gems, int cost, float damage, float attackSpeed, float critChance, float critDamage,
	List<Power> powers, Points points) {

	public static final int MAX_LEVEL = 5;
	public static final int MAX_PERK = 3;
	/** Emeralds one power level costs, and returns when the level is given up again. */
	public static final int UPGRADE_COST = 64;
	/** Cost of each level before its ±10% roll; all five come to about 500 gems. */
	private static final int[] BASE_COST = {60, 80, 100, 120, 140};
	private static final float COST_SPREAD = 0.1F;
	/** Attack speed every refinement level adds, +0.25 at level 5. */
	public static final float ATTACK_SPEED_PER_LEVEL = 0.05F;
	/** Experience levels for a weapon's second power, and how much each further one adds. */
	private static final int POWER_LEVELS = 20;
	private static final int POWER_LEVELS_STEP = 10;

	/** What one level point adds to the stat it is spent on: 3%. Attack speed takes none. */
	public static final float POINT_BOOST = 0.03F;

	/** The stats a level point can go to. */
	public enum Stat {
		DAMAGE, CRIT_CHANCE, CRIT_DAMAGE;

		public String id() {
			return this.name().toLowerCase(Locale.ROOT);
		}

		public static @Nullable Stat of(String id) {
			for (Stat stat : values()) {
				if (stat.id().equals(id)) {
					return stat;
				}
			}
			return null;
		}
	}

	/**
	 * Level points spent on each stat. Every refinement level earns the weapon one, on top of that level's
	 * random boost; the player spends it at the smithing table on the stat they want, {@link #POINT_BOOST}
	 * each: damage by 3% of the weapon's own, critical chance and critical damage by 3 points.
	 */
	public record Points(int damage, int critChance, int critDamage) {
		public static final Points NONE = new Points(0, 0, 0);
		public static final Codec<Points> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.optionalFieldOf("damage", 0).forGetter(Points::damage),
			Codec.INT.optionalFieldOf("critical_chance", 0).forGetter(Points::critChance),
			Codec.INT.optionalFieldOf("critical_damage", 0).forGetter(Points::critDamage)
		).apply(instance, Points::new));

		public int total() {
			return this.damage + this.critChance + this.critDamage;
		}

		public int of(Stat stat) {
			return switch (stat) {
				case DAMAGE -> this.damage;
				case CRIT_CHANCE -> this.critChance;
				case CRIT_DAMAGE -> this.critDamage;
			};
		}

		Points plus(Stat stat) {
			return switch (stat) {
				case DAMAGE -> new Points(this.damage + 1, this.critChance, this.critDamage);
				case CRIT_CHANCE -> new Points(this.damage, this.critChance + 1, this.critDamage);
				case CRIT_DAMAGE -> new Points(this.damage, this.critChance, this.critDamage + 1);
			};
		}
	}

	/** One trim power on a weapon: the trim's name, such as {@code bolt}, and the power's level. */
	public record Power(String trim, int level) {
		public static final Codec<Power> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.fieldOf("trim").forGetter(Power::trim),
			Codec.INT.fieldOf("level").forGetter(Power::level)
		).apply(instance, Power::new));

		public @Nullable Perk perk() {
			return Refinement.perk(this.trim);
		}
	}

	public static final Codec<Refinement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.INT.fieldOf("level").forGetter(Refinement::level),
		Codec.INT.optionalFieldOf("gems", 0).forGetter(Refinement::gems),
		Codec.INT.optionalFieldOf("cost", 0).forGetter(Refinement::cost),
		Codec.FLOAT.optionalFieldOf("damage", 0.0F).forGetter(Refinement::damage),
		Codec.FLOAT.optionalFieldOf("attack_speed", 0.0F).forGetter(Refinement::attackSpeed),
		Codec.FLOAT.optionalFieldOf("critical_chance", 0.0F).forGetter(Refinement::critChance),
		Codec.FLOAT.optionalFieldOf("critical_damage", 0.0F).forGetter(Refinement::critDamage),
		Power.CODEC.listOf().optionalFieldOf("powers", List.of()).forGetter(Refinement::powers),
		Points.CODEC.optionalFieldOf("points", Points.NONE).forGetter(Refinement::points),
		// Before several powers, a weapon held one trim and its level; read so old items keep their power.
		Codec.STRING.optionalFieldOf("trim").forGetter(refinement -> Optional.empty()),
		Codec.INT.optionalFieldOf("perk_level", 0).forGetter(refinement -> 0)
	).apply(instance, (level, gems, cost, damage, attackSpeed, critChance, critDamage, powers, points, trim, perkLevel) ->
		new Refinement(level, gems, cost, damage, attackSpeed, critChance, critDamage,
			powers.isEmpty() && trim.isPresent() ? List.of(new Power(trim.get(), Math.max(1, perkLevel))) : powers, points)));

	public static final DataComponentType<Refinement> COMPONENT = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
		TrimEnchantments.id("refinement"),
		DataComponentType.<Refinement>builder().persistent(CODEC).networkSynchronized(ByteBufCodecs.fromCodec(CODEC)).build());

	private static final Identifier BOOST_DAMAGE = TrimEnchantments.id("refinement_damage");
	private static final Identifier BOOST_ATTACK_SPEED = TrimEnchantments.id("refinement_attack_speed");
	private static final Identifier BOOST_CRITICAL_CHANCE = TrimEnchantments.id("refinement_critical_chance");
	private static final Identifier BOOST_CRITICAL_DAMAGE = TrimEnchantments.id("refinement_critical_damage");

	public Refinement {
		powers = List.copyOf(powers);
	}

	Refinement(int level, int gems, int cost, float damage, float attackSpeed, float critChance, float critDamage, List<Power> powers) {
		this(level, gems, cost, damage, attackSpeed, critChance, critDamage, powers, Points.NONE);
	}

	/** Level points earned and not spent yet: one per level. */
	public int unspentPoints() {
		return Math.max(0, this.level - this.points.total());
	}

	/**
	 * {@code stack} with the power of {@code trim} one level higher ({@code up}) or lower, or null if the
	 * weapon does not have it or it is already at the limit (level {@link #MAX_PERK}, or level 1 going down).
	 */
	public static @Nullable ItemStack shiftPower(ItemStack stack, String trim, boolean up) {
		Refinement refinement = stack.get(COMPONENT);
		if (!isRefinable(stack) || isShield(stack) || refinement == null) {
			return null;
		}
		List<Power> powers = new ArrayList<>(refinement.powers);
		for (int i = 0; i < powers.size(); i++) {
			Power power = powers.get(i);
			int level = power.level + (up ? 1 : -1);
			if (power.trim.equals(trim)) {
				if (level < 1 || level > MAX_PERK) {
					return null;
				}
				powers.set(i, new Power(power.trim, level));
				return refinement.withPowers(powers).applyTo(stack);
			}
		}
		return null;
	}

	/**
	 * {@code stack} with one more level point spent on {@code stat}, or null if it has none to spend.
	 * Spent points stay: they are never refunded.
	 */
	public static @Nullable ItemStack spendPoint(ItemStack stack, Stat stat) {
		Refinement refinement = stack.get(COMPONENT);
		if (!isWeapon(stack) || refinement == null || refinement.unspentPoints() <= 0) {
			return null;
		}
		return new Refinement(refinement.level, refinement.gems, refinement.cost, refinement.damage, refinement.attackSpeed,
			refinement.critChance, refinement.critDamage, refinement.powers, refinement.points.plus(stat)).applyTo(stack);
	}

	/**
	 * The power a trim grants, counted as that power's level by {@link Perks#level}. {@code built} is
	 * false for a power that is designed but not in the game yet; its trim is refused until it is.
	 */
	public enum Perk {
		LIGHTNING(Perks.LIGHTNING, true),
		SHOCKWAVE(Perks.SHOCKWAVE, true),
		COLD_EDGE(Perks.COLD_EDGE, true),
		UPPERCUT(Perks.UPPERCUT, true),
		CHAINS(Perks.CHAINS, true),
		ECHO(Perks.ECHO, true),
		WIND_CHARGED(Perks.WIND_CHARGED, true),
		GRAVITY(Perks.GRAVITY, true),
		MOMENTUM(Perks.MOMENTUM, true),
		RAMPAGING(Perks.RAMPAGING, true),
		VENOM(Perks.VENOM, false),
		SANDSTORM(Perks.SANDSTORM, false),
		PLUNDER(Perks.PLUNDER, false),
		WITHERING(Perks.WITHERING, false),
		PHASE(Perks.PHASE, false),
		SHARED_PAIN(Perks.SHARED_PAIN, false),
		GUARDING_STRIKE(Perks.GUARDING_STRIKE, false),
		DAZE(Perks.DAZE, false);

		final ResourceKey<Enchantment> enchantment;
		final boolean built;

		Perk(ResourceKey<Enchantment> enchantment, boolean built) {
			this.enchantment = enchantment;
			this.built = built;
		}

		public ResourceKey<Enchantment> enchantment() {
			return this.enchantment;
		}

		public String id() {
			return this.enchantment.identifier().getPath();
		}

		public boolean built() {
			return this.built;
		}

		public MutableComponent displayName() {
			return Component.translatable("enchantment.bcenchantments." + this.id());
		}

		public static @Nullable Perk of(ResourceKey<Enchantment> key) {
			for (Perk perk : values()) {
				if (perk.enchantment.equals(key)) {
					return perk;
				}
			}
			return null;
		}
	}

	/**
	 * Which power each vanilla trim grants, one each, by its name, its look or the structure it is
	 * found in; {@code POWERS.md} gives the reasons.
	 */
	private static final Map<String, Perk> TRIM_PERKS = Map.ofEntries(
		Map.entry("bolt", Perk.LIGHTNING),
		Map.entry("flow", Perk.WIND_CHARGED),
		Map.entry("tide", Perk.COLD_EDGE),
		Map.entry("silence", Perk.ECHO),
		Map.entry("ward", Perk.SHOCKWAVE),
		Map.entry("vex", Perk.CHAINS),
		Map.entry("eye", Perk.GRAVITY),
		Map.entry("wayfinder", Perk.MOMENTUM),
		Map.entry("snout", Perk.RAMPAGING),
		Map.entry("raiser", Perk.UPPERCUT),
		Map.entry("wild", Perk.VENOM),
		Map.entry("dune", Perk.SANDSTORM),
		Map.entry("coast", Perk.PLUNDER),
		Map.entry("rib", Perk.WITHERING),
		Map.entry("spire", Perk.PHASE),
		Map.entry("host", Perk.SHARED_PAIN),
		Map.entry("sentry", Perk.GUARDING_STRIKE),
		Map.entry("shaper", Perk.DAZE)
	);
	/** The powers a shield can take from a trim, each working on a blocked hit; see {@code ShieldPowers}. */
	private static final Set<Perk> SHIELD_PERKS = EnumSet.of(Perk.WIND_CHARGED, Perk.COLD_EDGE);
	private static final String TEMPLATE_SUFFIX = "_armor_trim_smithing_template";

	/**
	 * What taking the smithing table's result does: the weapon it hands out, how many items it takes
	 * from the addition slot, whether it uses up the template, and the experience levels it costs.
	 */
	public record Plan(ItemStack result, int additionItems, boolean consumesTemplate, boolean levelUp, int levelCost) {
		Plan(ItemStack result, int additionItems, boolean consumesTemplate, boolean levelUp) {
			this(result, additionItems, consumesTemplate, levelUp, 0);
		}
	}

	public static final TagKey<Item> REFINABLE = TagKey.create(Registries.ITEM, TrimEnchantments.id("refinable"));

	public static void init() {
		// Loading the class registers the component.
	}

	public static boolean isWeapon(ItemStack stack) {
		return CriticalHits.baseChance(stack.getItem()) > 0.0;
	}

	/**
	 * Weapons the smithing table refines and gives trim powers: diamond and netherite ones, the mace
	 * and the trident ({@code bcenchantments:refinable}). Lesser weapons keep whatever they already have.
	 */
	public static boolean isRefinable(ItemStack stack) {
		return isWeapon(stack) && stack.is(REFINABLE);
	}

	public static boolean isShield(ItemStack stack) {
		return !isWeapon(stack) && stack.has(DataComponents.BLOCKS_ATTACKS);
	}

	/** Items the smithing table's base slot takes beyond vanilla's: weapons and shields. */
	public static boolean isBase(ItemStack stack) {
		return isWeapon(stack) || isShield(stack);
	}

	/** Whether a shield can take {@code perk} from a trim. */
	public static boolean onShield(@Nullable Perk perk) {
		return perk != null && SHIELD_PERKS.contains(perk);
	}

	/** The trims whose power works on a shield, alphabetically. */
	public static List<String> shieldTrims(Perk perk) {
		return onShield(perk) ? trims(perk) : List.of();
	}

	public static int gemValue(ItemStack stack) {
		if (stack.is(Items.EMERALD)) {
			return 1;
		}
		return stack.is(Items.EMERALD_BLOCK) ? 9 : 0;
	}

	/** Items the smithing table's addition slot takes beyond vanilla's: gems and echo shards. */
	public static boolean isAddition(ItemStack stack) {
		return gemValue(stack) > 0 || stack.is(Items.ECHO_SHARD);
	}

	/** The trims that grant {@code perk}, alphabetically. */
	public static List<String> trims(Perk perk) {
		return TRIM_PERKS.entrySet().stream().filter(entry -> entry.getValue() == perk).map(Map.Entry::getKey).sorted().toList();
	}

	/** The trim that grants {@code perk}. */
	public static String trim(Perk perk) {
		return trims(perk).getFirst();
	}

	/** The armor trim smithing template item of {@code trim}. */
	public static Identifier template(String trim) {
		return Identifier.withDefaultNamespace(trim + TEMPLATE_SUFFIX);
	}

	/** A trim's name as players read it: {@code bolt} becomes Bolt. */
	public static String trimName(String trim) {
		return trim.substring(0, 1).toUpperCase(Locale.ROOT) + trim.substring(1);
	}

	/** Refinement level of {@code stack}, 0 if it was never refined. */
	public static int levelOf(ItemStack stack) {
		Refinement refinement = stack.get(COMPONENT);
		return refinement == null ? 0 : refinement.level;
	}

	/** The trim powers on {@code stack}, in the order they were applied. */
	public static List<Power> powersOf(ItemStack stack) {
		Refinement refinement = stack.get(COMPONENT);
		return refinement == null ? List.of() : refinement.powers;
	}

	public static @Nullable Perk perk(String trim) {
		return TRIM_PERKS.get(trim);
	}

	/** The trim of a trim smithing template, or null if {@code template} is none. */
	public static @Nullable String trimOf(ItemStack template) {
		String path = BuiltInRegistries.ITEM.getKey(template.getItem()).getPath();
		if (!path.endsWith(TEMPLATE_SUFFIX)) {
			return null;
		}
		String trim = path.substring(0, path.length() - TEMPLATE_SUFFIX.length());
		return TRIM_PERKS.containsKey(trim) ? trim : null;
	}

	private @Nullable Power power(Perk perk) {
		for (Power power : this.powers) {
			if (power.perk() == perk) {
				return power;
			}
		}
		return null;
	}

	/** Level of the power {@code key} granted by the stack's trims, 0 if it has none. */
	public static int perkLevel(ItemStack stack, ResourceKey<Enchantment> key) {
		for (Power power : powersOf(stack)) {
			Perk perk = power.perk();
			if (perk != null && key.equals(perk.enchantment)) {
				return power.level;
			}
		}
		return 0;
	}

	/** Whether the powers {@code a} and {@code b} on {@code stack} are combined with each other. */
	public static boolean combined(ItemStack stack, ResourceKey<Enchantment> a, ResourceKey<Enchantment> b) {
		List<Power> powers = powersOf(stack);
		int first = indexOf(powers, a);
		int second = indexOf(powers, b);
		return first >= 0 && second >= 0 && first != second && first / 2 == second / 2;
	}

	private static int indexOf(List<Power> powers, ResourceKey<Enchantment> key) {
		for (int i = 0; i < powers.size(); i++) {
			Perk perk = powers.get(i).perk();
			if (perk != null && key.equals(perk.enchantment)) {
				return i;
			}
		}
		return -1;
	}

	/** The power the next trim on {@code stack} would combine with, or null if it would start a new pair. */
	public static @Nullable Perk unpaired(ItemStack stack) {
		List<Power> powers = powersOf(stack);
		return powers.size() % 2 == 1 ? powers.getLast().perk() : null;
	}

	/**
	 * Why {@code perk} cannot go on {@code base} next to {@code taken} (the powers already chosen), as a
	 * language key, or null if it can: a shield takes one of its few powers, the Uppercut needs a weapon that
	 * launches, and a power that is not built yet or already chosen is refused.
	 */
	public static @Nullable String lockedReason(Perk perk, ItemStack base, List<Power> taken) {
		if (!perk.built) {
			return "forge.bcenchantments.locked.unbuilt";
		}
		for (Power power : taken) {
			if (power.perk() == perk) {
				return null;
			}
		}
		if (isShield(base)) {
			if (!onShield(perk)) {
				return "forge.bcenchantments.locked.shield";
			}
			return taken.isEmpty() ? null : "forge.bcenchantments.locked.shield_one";
		}
		if (perk == Perk.UPPERCUT && !launches(base)) {
			return "forge.bcenchantments.locked.uppercut";
		}
		return null;
	}

	/** Whether {@code base} is a weapon whose weapon style launches: swords, spears and the mace. */
	private static boolean launches(ItemStack base) {
		return base.is(ItemTags.SWORDS) || base.getItem() instanceof MaceItem
			|| BuiltInRegistries.ITEM.getKey(base.getItem()).getPath().endsWith("_spear");
	}

	/**
	 * A fully refined copy of {@code base} carrying {@code powers} in that order, as if every trim had
	 * been applied at the smithing table; null if the weapon or any power is refused (see
	 * {@link #lockedReason}), a power repeats, or a level is out of range.
	 */
	public static @Nullable ItemStack forge(ItemStack base, List<Power> powers) {
		if (!isBase(base)) {
			return null;
		}
		List<Power> taken = new ArrayList<>();
		for (Power power : powers) {
			Perk perk = power.perk();
			if (perk == null || power.level < 1 || power.level > (isShield(base) ? 1 : MAX_PERK)
				|| taken.stream().anyMatch(other -> other.perk() == perk)
				|| lockedReason(perk, base, isShield(base) ? List.of() : taken) != null) {
				return null;
			}
			taken.add(power);
		}
		if (isShield(base)) {
			if (taken.size() > 1) {
				return null;
			}
			ItemStack result = base.copyWithCount(1);
			result.set(COMPONENT, new Refinement(0, 0, 0, 0.0F, 0.0F, 0.0F, 0.0F, taken));
			return result;
		}
		return new Refinement(MAX_LEVEL, 0, 0, 0.0F, 0.0F, 0.0F, 0.0F, taken).applyTo(base);
	}

	/** Experience levels the next new power on a weapon holding {@code powers} costs: the first is free. */
	public static int powerCost(int powers) {
		return powers == 0 ? 0 : POWER_LEVELS + POWER_LEVELS_STEP * (powers - 1);
	}

	/**
	 * The smithing table's three inputs as a refinement step, or null if they are not one.
	 * {@code seed} makes the random rolls stable while the player looks at the result.
	 */
	public static @Nullable Plan plan(ItemStack template, ItemStack base, ItemStack addition, int seed) {
		if (isShield(base)) {
			return shieldPlan(template, base, addition);
		}
		if (!isRefinable(base)) {
			return null;
		}
		Refinement current = base.get(COMPONENT);
		if (template.isEmpty() && gemValue(addition) > 0) {
			Refinement from = current != null ? current : fresh(seed, base.getItem());
			return from.level < MAX_LEVEL ? from.pay(base, addition, seed) : null;
		}
		String trim = trimOf(template);
		if (trim == null || current == null || current.level < MAX_LEVEL) {
			return null;
		}
		Perk perk = perk(trim);
		if (perk == null || !perk.built) {
			return null;
		}
		if (current.power(perk) != null) {
			// A power the weapon has is raised with emeralds from the panel ({@link #shiftPower}), never by a second trim.
			return null;
		}
		if (!addition.is(Items.ECHO_SHARD)) {
			return null;
		}
		List<Power> added = new ArrayList<>(current.powers);
		added.add(new Power(trim, 1));
		return new Plan(current.withPowers(added).applyTo(base), 1, true, true, powerCost(current.powers.size()));
	}

	/**
	 * A shield needs no refinement: a trim template of a shield power and an echo shard grant it once,
	 * and there are no higher levels or further powers.
	 */
	private static @Nullable Plan shieldPlan(ItemStack template, ItemStack base, ItemStack addition) {
		String trim = trimOf(template);
		if (trim == null || !onShield(perk(trim)) || !powersOf(base).isEmpty() || !addition.is(Items.ECHO_SHARD)) {
			return null;
		}
		ItemStack result = base.copyWithCount(1);
		result.set(COMPONENT, new Refinement(0, 0, 0, 0.0F, 0.0F, 0.0F, 0.0F, List.of(new Power(trim, 1))));
		return new Plan(result, 1, true, true);
	}

	/**
	 * The refinement on {@code base}, or for a weapon never refined, the one its first emerald would
	 * start, with the cost {@code seed} rolls for it; null if {@code base} is not a weapon.
	 */
	public static @Nullable Refinement current(ItemStack base, int seed) {
		if (!isWeapon(base)) {
			return null;
		}
		Refinement refinement = base.get(COMPONENT);
		return refinement != null ? refinement : fresh(seed, base.getItem());
	}

	private static Refinement fresh(int seed, Item item) {
		return new Refinement(0, 0, rollCost(0, random(seed, item, 0)), 0.0F, 0.0F, 0.0F, 0.0F, List.of());
	}

	private Plan pay(ItemStack base, ItemStack addition, int seed) {
		int value = gemValue(addition);
		int needed = this.cost - this.gems;
		int items = Math.min(addition.getCount(), Mth.positiveCeilDiv(needed, value));
		int paid = this.gems + items * value;
		if (paid < this.cost) {
			Refinement partial = new Refinement(this.level, paid, this.cost, this.damage, this.attackSpeed, this.critChance, this.critDamage, this.powers, this.points);
			return new Plan(partial.applyTo(base), items, false, false);
		}

		RandomSource random = random(seed, base.getItem(), this.level + 1);
		int next = this.level + 1;
		float damage = this.damage;
		float attackSpeed = this.attackSpeed;
		float critChance = this.critChance;
		float critDamage = this.critDamage;
		// One small boost per level, so a finished weapon is a little better, never a different weapon.
		switch (random.nextInt(3)) {
			case 0 -> damage += Mth.randomBetween(random, 0.25F, 0.5F);
			case 1 -> critChance += Mth.randomBetween(random, 0.01F, 0.025F);
			default -> critDamage += Mth.randomBetween(random, 0.05F, 0.1F);
		}
		// Gems paid past the cost carry over; the last level has nothing to carry them into.
		int carried = next < MAX_LEVEL ? paid - this.cost : 0;
		int cost = next < MAX_LEVEL ? rollCost(next, random) : 0;
		Refinement raised = new Refinement(next, carried, cost, damage, attackSpeed, critChance, critDamage, this.powers, this.points);
		return new Plan(raised.applyTo(base), items, false, true);
	}

	private Refinement withPowers(List<Power> powers) {
		return new Refinement(this.level, this.gems, this.cost, this.damage, this.attackSpeed, this.critChance, this.critDamage, powers, this.points);
	}

	private static RandomSource random(int seed, Item item, int level) {
		return RandomSource.create((long) seed << 32 ^ BuiltInRegistries.ITEM.getKey(item).hashCode() * 31L + level);
	}

	private static int rollCost(int level, RandomSource random) {
		float spread = 1.0F + Mth.randomBetween(random, -COST_SPREAD, COST_SPREAD);
		return Math.round(BASE_COST[level] * spread);
	}

	/** A copy of {@code base} carrying this refinement, its boosts folded into the weapon's own stats. */
	private ItemStack applyTo(ItemStack base) {
		ItemStack result = base.copyWithCount(1);
		result.set(COMPONENT, this);
		// Attack speed comes with the level itself; the stored attackSpeed is left over from random boosts and ignored.
		float attackSpeed = ATTACK_SPEED_PER_LEVEL * this.level;

		ItemAttributeModifiers defaults = base.getItem().components().getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
		// Level points add to the random boosts: damage by a share of the weapon's own (the player's 1 included), the critical stats by points.
		double ownDamage = 1.0;
		for (ItemAttributeModifiers.Entry entry : defaults.modifiers()) {
			if (entry.matches(Attributes.ATTACK_DAMAGE, Item.BASE_ATTACK_DAMAGE_ID)) {
				ownDamage += entry.modifier().amount();
			}
		}
		float damage = this.damage + (float) (ownDamage * POINT_BOOST * this.points.damage);
		float critChance = this.critChance + POINT_BOOST * this.points.critChance;
		float critDamage = this.critDamage + POINT_BOOST * this.points.critDamage;
		ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
		boolean[] folded = new boolean[3];
		for (ItemAttributeModifiers.Entry entry : defaults.modifiers()) {
			// Boosts join the weapon's base lines, so the tooltip shows one total per stat instead of a pile of small additions.
			if (entry.matches(Attributes.ATTACK_DAMAGE, Item.BASE_ATTACK_DAMAGE_ID) && damage != 0.0F) {
				builder.add(entry.attribute(), plus(entry.modifier(), damage), entry.slot(), entry.display());
				folded[0] = true;
			} else if (entry.matches(Attributes.ATTACK_SPEED, Item.BASE_ATTACK_SPEED_ID) && attackSpeed != 0.0F) {
				builder.add(entry.attribute(), plus(entry.modifier(), attackSpeed), entry.slot(), entry.display());
				folded[1] = true;
			} else if (entry.matches(CriticalHits.CRITICAL_CHANCE, CriticalHits.WEAPON_CHANCE) && critChance != 0.0F) {
				AttributeModifier modifier = plus(entry.modifier(), critChance);
				builder.add(entry.attribute(), modifier, entry.slot(), ItemAttributeModifiers.Display.override(CriticalHits.chanceLine(modifier.amount())));
				folded[2] = true;
			} else {
				builder.add(entry.attribute(), entry.modifier(), entry.slot(), entry.display());
			}
		}
		addUnfolded(builder, !folded[0], Attributes.ATTACK_DAMAGE, BOOST_DAMAGE, damage);
		addUnfolded(builder, !folded[1], Attributes.ATTACK_SPEED, BOOST_ATTACK_SPEED, attackSpeed);
		addUnfolded(builder, !folded[2], CriticalHits.CRITICAL_CHANCE, BOOST_CRITICAL_CHANCE, critChance);
		if (critDamage != 0.0F) {
			Component line = Component.translatable("attribute.modifier.bcenchantments.critical_damage",
				CriticalHits.PERCENT.format(critDamage * 100.0) + "%").withStyle(ChatFormatting.BLUE);
			builder.add(CriticalHits.CRITICAL_DAMAGE, new AttributeModifier(BOOST_CRITICAL_DAMAGE, critDamage, AttributeModifier.Operation.ADD_VALUE),
				EquipmentSlotGroup.MAINHAND, ItemAttributeModifiers.Display.override(line));
		}
		result.set(DataComponents.ATTRIBUTE_MODIFIERS, builder.build());
		return result;
	}

	private static AttributeModifier plus(AttributeModifier modifier, float amount) {
		return new AttributeModifier(modifier.id(), modifier.amount() + amount, modifier.operation());
	}

	/** A boost for a weapon whose defaults have no base line for that stat to join. */
	private static void addUnfolded(ItemAttributeModifiers.Builder builder, boolean needed, Holder<Attribute> attribute, Identifier id, float amount) {
		if (needed && amount != 0.0F) {
			builder.add(attribute, new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
		}
	}

	/**
	 * The refinement's tooltip lines, placed under the item's name. {@code details} adds each trim
	 * power's description and how to raise it.
	 */
	public static void tooltip(ItemStack stack, List<Component> lines, boolean details) {
		Refinement refinement = stack.get(COMPONENT);
		if (refinement == null || lines.isEmpty()) {
			return;
		}
		List<Component> added = new ArrayList<>();
		if (isShield(stack) && !refinement.powers.isEmpty()) {
			Power power = refinement.powers.getFirst();
			Perk perk = power.perk();
			if (perk != null) {
				added.add(Component.translatable("refinement.bcenchantments.trimmed", trimName(power.trim)).withStyle(ChatFormatting.GRAY));
				added.add(Component.literal(" ").append(perk.displayName()).withStyle(ChatFormatting.GRAY));
				added.add(hint(Component.translatable("refinement.bcenchantments.shield." + perk.id())));
			}
		} else if (!refinement.powers.isEmpty()) {
			added.add(Component.translatable("refinement.bcenchantments.powers").withStyle(ChatFormatting.GRAY));
			for (Power power : refinement.powers) {
				Perk perk = power.perk();
				if (perk == null) {
					continue;
				}
				added.add(Component.literal(" ").append(perk.displayName()).append(" ")
					.append(Component.translatable("enchantment.level." + power.level)).withStyle(ChatFormatting.GRAY));
				if (details) {
					Synergy.describe("tooltip.bcenchantments.power." + perk.id(), added);
					if (power.level < MAX_PERK) {
						added.add(hint(Component.translatable("refinement.bcenchantments.upgrade", UPGRADE_COST)));
					}
				}
			}
		} else if (refinement.level >= MAX_LEVEL) {
			added.add(level(refinement.level));
			added.add(hint(Component.translatable("refinement.bcenchantments.ready")));
		} else {
			added.add(level(refinement.level));
			added.add(hint(Component.translatable("refinement.bcenchantments.progress", refinement.gems, refinement.cost)));
		}
		if (refinement.unspentPoints() > 0) {
			added.add(hint(Component.translatable("refinement.bcenchantments.points", refinement.unspentPoints())));
		}
		lines.addAll(1, added);
	}

	private static Component level(int level) {
		MutableComponent line = level == 0
			? Component.translatable("refinement.bcenchantments.unrefined")
			: Component.translatable("refinement.bcenchantments.level", Component.translatable("enchantment.level." + level));
		return line.withStyle(ChatFormatting.GRAY);
	}

	private static Component hint(Component text) {
		return Component.literal(" ").append(text).withStyle(ChatFormatting.DARK_GRAY);
	}
}
