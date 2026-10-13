package dev.vadim.bcenchantments.mixin;

import java.util.Map;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Punchy hides spears from Better Combat in first person, by having the registry's lookup by item id
 * answer null for them, so it can play its own spear stab. That also cuts the server of a singleplayer
 * world off from them. The spear has its own combo now, so spears are looked up past that here.
 */
@Mixin(value = WeaponRegistry.class, remap = false)
public abstract class WeaponRegistryMixin {
	private static final TagKey<Item> SPEARS = TagKey.create(Registries.ITEM, Identifier.withDefaultNamespace("spears"));

	@Shadow
	static Map<Identifier, WeaponAttributes> registrations;

	@Inject(method = "getAttributes(Lnet/minecraft/world/item/ItemStack;)Lnet/bettercombat/api/WeaponAttributes;", at = @At("HEAD"), cancellable = true)
	private static void bcenchantments$spearAttributes(ItemStack stack, CallbackInfoReturnable<WeaponAttributes> cir) {
		if (stack == null || stack.isEmpty() || stack.has(BetterCombatDataComponents.WEAPON_PRESET_ID)) {
			return;
		}
		Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
		if (stack.is(SPEARS) || id.getPath().contains("spear")) {
			cir.setReturnValue(registrations.get(id));
		}
	}
}
