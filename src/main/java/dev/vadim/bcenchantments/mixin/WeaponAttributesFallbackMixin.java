package dev.vadim.bcenchantments.mixin;

import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.logic.WeaponAttributesFallback;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Better Combat's fallback only fills in items that have no attributes yet, and asks the registry's
 * lookup by item id. Punchy has that lookup answer null for spears, so the fallback would put its
 * generic spear over the spear combo. The fallback reads the registrations directly here instead.
 */
@Mixin(value = WeaponAttributesFallback.class, remap = false)
public abstract class WeaponAttributesFallbackMixin {
	@Redirect(method = "initialize", at = @At(value = "INVOKE", target = "Lnet/bettercombat/logic/WeaponRegistry;getAttributes(Lnet/minecraft/resources/Identifier;)Lnet/bettercombat/api/WeaponAttributes;"))
	private static WeaponAttributes bcenchantments$registered(Identifier itemId) {
		return WeaponRegistryAccessor.bcenchantments$registrations().get(itemId);
	}
}
