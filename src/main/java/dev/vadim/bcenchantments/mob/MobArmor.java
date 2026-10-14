package dev.vadim.bcenchantments.mob;

import dev.vadim.bcenchantments.TrimEnchantments;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/**
 * Armor for monsters, by the {@link Place.Tier} of where they spawn: a few copper pieces near home,
 * up to enchanted diamond and netherite where it is deadliest. Only monsters in the
 * {@code bcenchantments:armored_monsters} tag wear it, slots they already fill stay as they are,
 * and each piece drops rarely, so farms don't flood with armor.
 */
final class MobArmor {
	static final TagKey<EntityType<?>> WEARERS = TagKey.create(Registries.ENTITY_TYPE, TrimEnchantments.id("armored_monsters"));

	private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

	/** Copper up to netherite, each row in {@link #SLOTS} order. */
	private static final Item[][] SETS = {
		{Items.COPPER_HELMET, Items.COPPER_CHESTPLATE, Items.COPPER_LEGGINGS, Items.COPPER_BOOTS},
		{Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS},
		{Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS},
		{Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS},
		{Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS},
	};

	private MobArmor() {
	}

	static void equip(ServerLevel world, Mob mob, Place.Tier tier) {
		if (!mob.typeHolder().is(WEARERS)) {
			return;
		}
		int t = tier.ordinal();
		RandomSource random = mob.getRandom();
		if (random.nextDouble() >= MobBalance.ARMOR_WEAR_CHANCE[t]) {
			return;
		}
		// One material for the whole outfit, the better ones rarer within the tier's range.
		int low = MobBalance.ARMOR_MATERIAL_MIN[t];
		int high = MobBalance.ARMOR_MATERIAL_MAX[t];
		double roll = random.nextDouble();
		int material = Math.min(high, low + (int) (roll * roll * (high - low + 1)));
		// The helmet always comes; every other piece is its own roll.
		for (int i = 0; i < SLOTS.length; i++) {
			EquipmentSlot slot = SLOTS[i];
			if (slot != EquipmentSlot.HEAD && random.nextDouble() >= MobBalance.ARMOR_PIECE_CHANCE[t]) {
				continue;
			}
			if (!mob.getItemBySlot(slot).isEmpty()) {
				continue;
			}
			ItemStack piece = new ItemStack(SETS[material][i]);
			if (random.nextDouble() < MobBalance.ARMOR_ENCHANT_CHANCE[t]) {
				piece = EnchantmentHelper.enchantItem(random, piece, MobBalance.ARMOR_ENCHANT_LEVEL[t], world.registryAccess(), Optional.empty());
			}
			mob.setItemSlot(slot, piece);
			mob.setDropChance(slot, MobBalance.ARMOR_DROP_CHANCE);
		}
	}
}
