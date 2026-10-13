package dev.vadim.bcenchantments.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.vadim.bcenchantments.RpgAdditions;
import dev.vadim.bcenchantments.combat.CriticalHits;
import dev.vadim.bcenchantments.perk.Perks;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import org.jspecify.annotations.Nullable;

/**
 * Weapon refinement at the smithing table. Gems (emeralds, or blocks of emerald worth nine) are paid
 * into a weapon until it reaches the next refinement level; each level adds attack speed and one
 * small, random stat boost, and rolls what the next level will cost. At level {@link #MAX_LEVEL} the weapon is locked and
 * waits for a trim: a trim template and an echo shard turn it into a trim refinement, which grants one
 * power in place of an enchantment. Another trim of the same power raises that power, up to
 * {@link #MAX_PERK}. A shield skips the levels: a trim of one of the few shield powers and an echo
 * shard grant it straight away.
 *
 * @param level refinement level, 0 to {@link #MAX_LEVEL}
 * @param gems gems paid toward the next level
 * @param cost gems the next level costs, rolled when the previous one was reached
 * @param trim the trim pattern's name, such as {@code bolt}, once the weapon is trim refined
 */
public record Refinement(int level, int gems, int cost, float damage, float attackSpeed, float critChance, float critDamage,
	Optional<String> trim, int perkLevel) {

	public static final int MAX_LEVEL = 5;
	public static final int MAX_PERK = 3;
	/** Cost of each level before its ±10% roll; all five come to about 500 gems. */
	private static final int[] BASE_COST = {60, 80, 100, 120, 140};
	private static final float COST_SPREAD = 0.1F;
	/** Attack speed every refinement level adds; level 5 matches the old Attack Speed V. */
	private static final float ATTACK_SPEED_PER_LEVEL = 0.1F;

	public static final Codec<Refinement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.INT.fieldOf("level").forGetter(Refinement::level),
		Codec.INT.optionalFieldOf("gems", 0).forGetter(Refinement::gems),
		Codec.INT.optionalFieldOf("cost", 0).forGetter(Refinement::cost),
		Codec.FLOAT.optionalFieldOf("damage", 0.0F).forGetter(Refinement::damage),
		Codec.FLOAT.optionalFieldOf("attack_speed", 0.0F).forGetter(Refinement::attackSpeed),
		Codec.FLOAT.optionalFieldOf("critical_chance", 0.0F).forGetter(Refinement::critChance),
		Codec.FLOAT.optionalFieldOf("critical_damage", 0.0F).forGetter(Refinement::critDamage),
		Codec.STRING.optionalFieldOf("trim").forGetter(Refinement::trim),
		Codec.INT.optionalFieldOf("perk_level", 0).forGetter(Refinement::perkLevel)
	).apply(instance, Refinement::new));

	public static final DataComponentType<Refinement> COMPONENT = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
		RpgAdditions.id("refinement"),
		DataComponentType.<Refinement>builder().persistent(CODEC).networkSynchronized(ByteBufCodecs.fromCodec(CODEC)).build());

	private static final Identifier BOOST_DAMAGE = RpgAdditions.id("refinement_damage");
	private static final Identifier BOOST_ATTACK_SPEED = RpgAdditions.id("refinement_attack_speed");
	private static final Identifier BOOST_CRITICAL_CHANCE = RpgAdditions.id("refinement_critical_chance");
	private static final Identifier BOOST_CRITICAL_DAMAGE = RpgAdditions.id("refinement_critical_damage");

	/**
	 * The one power a trim refinement grants, counted as that enchantment's level by {@link Perks#level}.
	 */
	public enum Perk {
		LIGHTNING("enchantment.bcenchantments.lightning", Perks.LIGHTNING),
		SHOCKWAVE("enchantment.bcenchantments.shockwave", Perks.SHOCKWAVE),
		COLD_EDGE("enchantment.bcenchantments.cold_edge", Perks.COLD_EDGE),
		UPPERCUT("enchantment.bcenchantments.uppercut", Perks.UPPERCUT),
		CHAINS("enchantment.bcenchantments.chains", Perks.CHAINS),
		ECHO("enchantment.bcenchantments.echo", Perks.ECHO),
		WIND_CHARGED("enchantment.bcenchantments.wind_charged", Perks.WIND_CHARGED),
		GRAVITY("enchantment.bcenchantments.gravity", Perks.GRAVITY),
		MOMENTUM("enchantment.bcenchantments.momentum", Perks.MOMENTUM),
		RAMPAGING("enchantment.bcenchantments.rampaging", Perks.RAMPAGING);

		final String translationKey;
		final ResourceKey<Enchantment> enchantment;

		Perk(String translationKey, ResourceKey<Enchantment> enchantment) {
			this.translationKey = translationKey;
			this.enchantment = enchantment;
		}

		public ResourceKey<Enchantment> enchantment() {
			return this.enchantment;
		}
	}

	/**
	 * Which power each vanilla trim grants, by its name, its look or the structure it is found in: the
	 * bolt for Lightning, the breeze's flow for Wind Charged, the warden's ward for Shockwave's sonic
	 * boom, the eye of ender's pull for Gravity, the ravager-led raids of the sentry for Rampaging.
	 */
	private static final Map<String, Perk> TRIM_PERKS = Map.ofEntries(
		Map.entry("bolt", Perk.LIGHTNING),
		Map.entry("flow", Perk.WIND_CHARGED), Map.entry("dune", Perk.WIND_CHARGED),
		Map.entry("tide", Perk.COLD_EDGE), Map.entry("coast", Perk.COLD_EDGE),
		Map.entry("silence", Perk.ECHO),
		Map.entry("ward", Perk.SHOCKWAVE), Map.entry("rib", Perk.SHOCKWAVE),
		Map.entry("wild", Perk.CHAINS), Map.entry("vex", Perk.CHAINS),
		Map.entry("eye", Perk.GRAVITY), Map.entry("spire", Perk.GRAVITY),
		Map.entry("wayfinder", Perk.MOMENTUM), Map.entry("host", Perk.MOMENTUM),
		Map.entry("snout", Perk.RAMPAGING), Map.entry("sentry", Perk.RAMPAGING),
		Map.entry("raiser", Perk.UPPERCUT), Map.entry("shaper", Perk.UPPERCUT)
	);
	/** The powers a shield can take from a trim, each working on a blocked hit; see {@code ShieldPowers}. */
	private static final Set<Perk> SHIELD_PERKS = EnumSet.of(Perk.WIND_CHARGED, Perk.COLD_EDGE);
	private static final String TEMPLATE_SUFFIX = "_armor_trim_smithing_template";

	/**
	 * What taking the smithing table's result does: the weapon it hands out, how many items it takes
	 * from the addition slot, and whether it uses up the template.
	 */
	public record Plan(ItemStack result, int additionItems, boolean consumesTemplate, boolean levelUp) {
	}

	public static void init() {
		// Loading the class registers the component.
	}

	public static boolean isWeapon(ItemStack stack) {
		return CriticalHits.baseChance(stack.getItem()) > 0.0;
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

	/** The armor trim smithing template item of {@code trim}. */
	public static Identifier template(String trim) {
		return Identifier.withDefaultNamespace(trim + TEMPLATE_SUFFIX);
	}

	/** Refinement level of {@code stack}, 0 if it was never refined. */
	public static int levelOf(ItemStack stack) {
		Refinement refinement = stack.get(COMPONENT);
		return refinement == null ? 0 : refinement.level;
	}

	public static @Nullable Perk perk(String trim) {
		return TRIM_PERKS.get(trim);
	}

	private static @Nullable String trimOf(ItemStack template) {
		String path = BuiltInRegistries.ITEM.getKey(template.getItem()).getPath();
		if (!path.endsWith(TEMPLATE_SUFFIX)) {
			return null;
		}
		String trim = path.substring(0, path.length() - TEMPLATE_SUFFIX.length());
		return TRIM_PERKS.containsKey(trim) ? trim : null;
	}

	public @Nullable Perk perk() {
		return this.trim.map(Refinement::perk).orElse(null);
	}

	/** Level of the power {@code key} granted by the stack's trim refinement, 0 if it grants another or none. */
	public static int perkLevel(ItemStack stack, ResourceKey<Enchantment> key) {
		Refinement refinement = stack.get(COMPONENT);
		if (refinement == null) {
			return 0;
		}
		Perk perk = refinement.perk();
		return perk != null && key.equals(perk.enchantment) ? refinement.perkLevel : 0;
	}

	/**
	 * The smithing table's three inputs as a refinement step, or null if they are not one.
	 * {@code seed} makes the random rolls stable while the player looks at the result.
	 */
	public static @Nullable Plan plan(ItemStack template, ItemStack base, ItemStack addition, int seed) {
		if (isShield(base)) {
			return shieldPlan(template, base, addition);
		}
		if (!isWeapon(base)) {
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
		if (current.trim.isEmpty() && addition.is(Items.ECHO_SHARD)) {
			Refinement trimmed = current.withPerk(Optional.of(trim), 1);
			return new Plan(trimmed.applyTo(base), 1, true, true);
		}
		if (addition.isEmpty() && current.perk() == perk(trim) && current.perkLevel < MAX_PERK) {
			Refinement raised = current.withPerk(current.trim, current.perkLevel + 1);
			return new Plan(raised.applyTo(base), 0, true, true);
		}
		return null;
	}

	/**
	 * A shield needs no refinement: a trim template of a shield power and an echo shard grant it once,
	 * and there are no higher levels.
	 */
	private static @Nullable Plan shieldPlan(ItemStack template, ItemStack base, ItemStack addition) {
		String trim = trimOf(template);
		Refinement current = base.get(COMPONENT);
		if (trim == null || !onShield(perk(trim)) || current != null && current.trim.isPresent() || !addition.is(Items.ECHO_SHARD)) {
			return null;
		}
		ItemStack result = base.copyWithCount(1);
		result.set(COMPONENT, new Refinement(0, 0, 0, 0.0F, 0.0F, 0.0F, 0.0F, Optional.of(trim), 1));
		return new Plan(result, 1, true, true);
	}

	private static Refinement fresh(int seed, Item item) {
		return new Refinement(0, 0, rollCost(0, random(seed, item, 0)), 0.0F, 0.0F, 0.0F, 0.0F, Optional.empty(), 0);
	}

	private Plan pay(ItemStack base, ItemStack addition, int seed) {
		int value = gemValue(addition);
		int needed = this.cost - this.gems;
		int items = Math.min(addition.getCount(), Mth.positiveCeilDiv(needed, value));
		int paid = this.gems + items * value;
		if (paid < this.cost) {
			Refinement partial = new Refinement(this.level, paid, this.cost, this.damage, this.attackSpeed, this.critChance, this.critDamage, this.trim, this.perkLevel);
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
		Refinement raised = new Refinement(next, carried, cost, damage, attackSpeed, critChance, critDamage, this.trim, this.perkLevel);
		return new Plan(raised.applyTo(base), items, false, true);
	}

	private Refinement withPerk(Optional<String> trim, int perkLevel) {
		return new Refinement(this.level, this.gems, this.cost, this.damage, this.attackSpeed, this.critChance, this.critDamage, trim, perkLevel);
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
		ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
		boolean[] folded = new boolean[3];
		for (ItemAttributeModifiers.Entry entry : defaults.modifiers()) {
			// Boosts join the weapon's base lines, so the tooltip shows one total per stat instead of a pile of small additions.
			if (entry.matches(Attributes.ATTACK_DAMAGE, Item.BASE_ATTACK_DAMAGE_ID) && this.damage != 0.0F) {
				builder.add(entry.attribute(), plus(entry.modifier(), this.damage), entry.slot(), entry.display());
				folded[0] = true;
			} else if (entry.matches(Attributes.ATTACK_SPEED, Item.BASE_ATTACK_SPEED_ID) && attackSpeed != 0.0F) {
				builder.add(entry.attribute(), plus(entry.modifier(), attackSpeed), entry.slot(), entry.display());
				folded[1] = true;
			} else if (entry.matches(CriticalHits.CRITICAL_CHANCE, CriticalHits.WEAPON_CHANCE) && this.critChance != 0.0F) {
				AttributeModifier modifier = plus(entry.modifier(), this.critChance);
				builder.add(entry.attribute(), modifier, entry.slot(), ItemAttributeModifiers.Display.override(CriticalHits.chanceLine(modifier.amount())));
				folded[2] = true;
			} else {
				builder.add(entry.attribute(), entry.modifier(), entry.slot(), entry.display());
			}
		}
		addUnfolded(builder, !folded[0], Attributes.ATTACK_DAMAGE, BOOST_DAMAGE, this.damage);
		addUnfolded(builder, !folded[1], Attributes.ATTACK_SPEED, BOOST_ATTACK_SPEED, attackSpeed);
		addUnfolded(builder, !folded[2], CriticalHits.CRITICAL_CHANCE, BOOST_CRITICAL_CHANCE, this.critChance);
		if (this.critDamage != 0.0F) {
			Component line = Component.translatable("attribute.modifier.bcenchantments.critical_damage",
				CriticalHits.PERCENT.format(this.critDamage * 100.0) + "%").withStyle(ChatFormatting.BLUE);
			builder.add(CriticalHits.CRITICAL_DAMAGE, new AttributeModifier(BOOST_CRITICAL_DAMAGE, this.critDamage, AttributeModifier.Operation.ADD_VALUE),
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

	/** The refinement's tooltip lines, placed under the item's name. */
	public static void tooltip(ItemStack stack, List<Component> lines) {
		Refinement refinement = stack.get(COMPONENT);
		if (refinement == null || lines.isEmpty()) {
			return;
		}
		List<Component> added = new ArrayList<>();
		Perk perk = refinement.perk();
		if (perk != null) {
			String trim = refinement.trim.get();
			String name = trim.substring(0, 1).toUpperCase(Locale.ROOT) + trim.substring(1);
			added.add(Component.translatable("refinement.bcenchantments.trimmed", name).withStyle(ChatFormatting.GRAY));
			if (isShield(stack)) {
				added.add(Component.literal(" ").append(Component.translatable(perk.translationKey)).withStyle(ChatFormatting.GRAY));
				added.add(hint(Component.translatable("refinement.bcenchantments.shield." + perk.enchantment.identifier().getPath())));
			} else {
				added.add(Component.literal(" ").append(Component.translatable(perk.translationKey)).append(" ")
					.append(Component.translatable("enchantment.level." + refinement.perkLevel)).withStyle(ChatFormatting.GRAY));
			}
			if (refinement.perkLevel < MAX_PERK && isWeapon(stack)) {
				added.add(hint(Component.translatable("refinement.bcenchantments.upgrade", name)));
			}
		} else if (refinement.level >= MAX_LEVEL) {
			added.add(level(refinement.level));
			added.add(hint(Component.translatable("refinement.bcenchantments.ready")));
		} else {
			added.add(level(refinement.level));
			added.add(hint(Component.translatable("refinement.bcenchantments.progress", refinement.gems, refinement.cost)));
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
