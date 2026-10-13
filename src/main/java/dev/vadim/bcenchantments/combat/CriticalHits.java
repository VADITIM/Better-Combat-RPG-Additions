package dev.vadim.bcenchantments.combat;

import dev.vadim.bcenchantments.TrimEnchantments;
import dev.vadim.bcenchantments.network.CritRollPayload;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * Critical hits by chance instead of by falling. Every weapon carries a critical chance as an
 * attribute modifier, so later upgrades only have to add to it. Vanilla's jump crit is gone: whether
 * a swing crits is rolled once per hand ahead of time and synced to the client, so the client already
 * knows while choosing the swing's animation (a sword swung in the air on a crit becomes the
 * two-handed air crit), and every hit of one Better Combat swing shares the same roll.
 */
public final class CriticalHits {
	public static final Holder<Attribute> CRITICAL_CHANCE = register("critical_chance",
		new RangedAttribute("attribute.name.bcenchantments.critical_chance", 0.0, 0.0, 1.0));
	public static final Holder<Attribute> CRITICAL_DAMAGE = register("critical_damage",
		new RangedAttribute("attribute.name.bcenchantments.critical_damage", 1.5, 1.0, 10.0));

	public static final Identifier WEAPON_CHANCE = TrimEnchantments.id("base_critical_chance");

	/** Rolls in [0, 1) waiting for each player's next main-hand and off-hand swing. */
	private static final Map<ServerPlayer, float[]> ROLLS = new WeakHashMap<>();
	public static final DecimalFormat PERCENT = new DecimalFormat("0.#");
	private static final RandomSource RANDOM = RandomSource.create();

	/** The rolls the server last sent this client, main hand first. */
	private static float clientMainRoll = 1.0F;
	private static float clientOffRoll = 1.0F;

	private static ServerPlayer swingPlayer;
	private static boolean swingCritical;

	private CriticalHits() {
	}

	private static Holder<Attribute> register(String name, Attribute attribute) {
		return Registry.registerForHolder(BuiltInRegistries.ATTRIBUTE, TrimEnchantments.id(name), attribute.setSyncable(true));
	}

	public static void init() {
		DefaultItemComponentEvents.MODIFY.register(context -> context.modify(item -> baseChance(item) > 0.0, (builder, item) -> {
			double chance = baseChance(item);
			ItemAttributeModifiers modifiers = item.components().getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
			ItemAttributeModifiers.Builder rebuilt = ItemAttributeModifiers.builder();
			for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
				rebuilt.add(entry.attribute(), entry.modifier(), entry.slot(), entry.display());
			}
			// The default tooltip line would read "+0.1"; this one reads as a percentage, like the weapon's other stats.
			rebuilt.add(CRITICAL_CHANCE, new AttributeModifier(WEAPON_CHANCE, chance, AttributeModifier.Operation.ADD_VALUE),
				EquipmentSlotGroup.MAINHAND, ItemAttributeModifiers.Display.override(chanceLine(chance)));
			builder.set(DataComponents.ATTRIBUTE_MODIFIERS, rebuilt.build());
		}));
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sync(handler.player));
	}

	/** The weapon's own critical chance as a tooltip line, in the dark green of its other base stats. */
	public static Component chanceLine(double chance) {
		return Component.literal(" ")
			.append(Component.translatable("attribute.modifier.bcenchantments.critical_chance", PERCENT.format(chance * 100.0) + "%"))
			.withStyle(ChatFormatting.DARK_GREEN);
	}

	/**
	 * Critical chance a weapon starts with, by kind: light, precise weapons crit most. Picked by the
	 * item's name so weapons from other mods that follow vanilla naming get one too.
	 */
	public static double baseChance(Item item) {
		String path = BuiltInRegistries.ITEM.getKey(item).getPath();
		if (path.endsWith("_sword")) {
			return 0.10;
		}
		if (path.endsWith("_spear")) {
			return 0.08;
		}
		if (path.endsWith("_axe") || path.equals("trident") || path.equals("mace")) {
			return 0.05;
		}
		return 0.0;
	}

	/** Critical chance of a swing with {@code stack}, from either hand. */
	public static double chance(Player player, ItemStack stack) {
		// The player's value already counts whatever is in the main hand, which may not be the weapon swinging.
		double chance = player.getAttributeValue(CRITICAL_CHANCE) - ownChance(player.getMainHandItem()) + ownChance(stack);
		return Math.clamp(chance, 0.0, 1.0);
	}

	private static double ownChance(ItemStack stack) {
		double[] sum = {0.0};
		stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).forEach(EquipmentSlotGroup.MAINHAND, (attribute, modifier) -> {
			if (attribute.equals(CRITICAL_CHANCE) && modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
				sum[0] += modifier.amount();
			}
		});
		return sum[0];
	}

	/** Whether the next swing with the given hand will crit, on either side. */
	public static boolean next(Player player, boolean offHand) {
		ItemStack stack = offHand ? player.getOffhandItem() : player.getMainHandItem();
		float roll;
		if (player instanceof ServerPlayer serverPlayer) {
			roll = rolls(serverPlayer)[offHand ? 1 : 0];
		} else {
			roll = offHand ? clientOffRoll : clientMainRoll;
		}
		return roll < chance(player, stack);
	}

	public static void receive(CritRollPayload payload) {
		clientMainRoll = payload.mainHand();
		clientOffRoll = payload.offHand();
	}

	static void beginSwing(ServerPlayer player, boolean offHand) {
		swingPlayer = player;
		swingCritical = next(player, offHand);
	}

	/** The swing used its roll, so that hand gets a fresh one. */
	static void endSwing(ServerPlayer player, boolean offHand) {
		swingPlayer = null;
		rolls(player)[offHand ? 1 : 0] = RANDOM.nextFloat();
		sync(player);
	}

	/** Replaces vanilla's falling check in {@code Player.attack}. */
	public static boolean isCritical(ServerPlayer player, Entity target) {
		if (!(target instanceof LivingEntity)) {
			return false;
		}
		if (player == swingPlayer) {
			return swingCritical;
		}
		// A plain vanilla attack, outside any Better Combat swing, rolls on the spot.
		return RANDOM.nextFloat() < chance(player, player.getMainHandItem());
	}

	public static float damageMultiplier(Player player) {
		return (float) player.getAttributeValue(CRITICAL_DAMAGE);
	}

	private static float[] rolls(ServerPlayer player) {
		return ROLLS.computeIfAbsent(player, p -> new float[] {RANDOM.nextFloat(), RANDOM.nextFloat()});
	}

	private static void sync(ServerPlayer player) {
		if (ServerPlayNetworking.canSend(player, CritRollPayload.TYPE)) {
			float[] rolls = rolls(player);
			ServerPlayNetworking.send(player, new CritRollPayload(rolls[0], rolls[1]));
		}
	}
}
