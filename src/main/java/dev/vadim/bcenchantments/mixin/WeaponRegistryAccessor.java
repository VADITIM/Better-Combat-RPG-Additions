package dev.vadim.bcenchantments.mixin;

import java.util.Map;
import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.logic.WeaponRegistry;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = WeaponRegistry.class, remap = false)
public interface WeaponRegistryAccessor {
	@Accessor("registrations")
	static Map<Identifier, WeaponAttributes> bcenchantments$registrations() {
		throw new AssertionError();
	}
}
