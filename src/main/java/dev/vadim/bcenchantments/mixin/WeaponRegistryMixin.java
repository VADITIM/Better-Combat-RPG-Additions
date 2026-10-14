package dev.vadim.bcenchantments.mixin;

import dev.vadim.bcenchantments.TrimEnchantments;
import dev.vadim.bcenchantments.combat.BetterCombatSwitch;
import java.util.Map;
import net.bettercombat.api.AttributesContainer;
import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.api.component.BetterCombatDataComponents;
import net.bettercombat.logic.WeaponRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Punchy hides spears from Better Combat in first person, by having the registry's lookup by item id
 * answer null for them, so it can play its own spear stab. That also cuts the server of a singleplayer
 * world off from them. The spear has its own combo now, so spears are looked up past that here.
 * With {@link BetterCombatSwitch} off, nothing has attributes at all.
 */
@Mixin(value = WeaponRegistry.class, remap = false)
public abstract class WeaponRegistryMixin {
	private static final TagKey<Item> SPEARS = TagKey.create(Registries.ITEM, Identifier.withDefaultNamespace("spears"));

	private static final Identifier TRIDENT = Identifier.withDefaultNamespace("trident");
	private static final String BETTERCOMBAT_TRIDENT = "bettercombat:trident";

	@Shadow
	static Map<Identifier, WeaponAttributes> registrations;

	/**
	 * Better Combat ships {@code minecraft:trident} as its own one-stab trident. Which mod's file wins
	 * depends on pack order, so the trident is pointed at this mod's combo here instead.
	 */
	@ModifyVariable(method = "resolveAttributes", at = @At("HEAD"), argsOnly = true)
	private static AttributesContainer bcenchantments$tridentCombo(AttributesContainer container, Identifier itemId) {
		return TRIDENT.equals(itemId) && container != null && BETTERCOMBAT_TRIDENT.equals(container.parent())
			? new AttributesContainer(TrimEnchantments.MOD_ID + ":trident", container.attributes())
			: container;
	}

	@Inject(method = "getAttributes(Lnet/minecraft/world/item/ItemStack;)Lnet/bettercombat/api/WeaponAttributes;", at = @At("HEAD"), cancellable = true)
	private static void bcenchantments$spearAttributes(ItemStack stack, CallbackInfoReturnable<WeaponAttributes> cir) {
		if (BetterCombatSwitch.disabled()) {
			cir.setReturnValue(null);
			return;
		}
		if (stack == null || stack.isEmpty() || stack.has(BetterCombatDataComponents.WEAPON_PRESET_ID)) {
			return;
		}
		Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
		if (stack.is(SPEARS) || id.getPath().contains("spear")) {
			cir.setReturnValue(registrations.get(id));
		}
	}
}
