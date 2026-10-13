package dev.vadim.bcenchantments.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.vadim.bcenchantments.mob.SpawnDensity;
import net.minecraft.world.entity.MobCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Raises the monster spawn cap; see {@link SpawnDensity}. */
@Mixin(MobCategory.class)
public abstract class MobCategoryMixin {
	@ModifyReturnValue(method = "getMaxInstancesPerChunk", at = @At("RETURN"))
	private int bcenchantments$denserMonsters(int vanilla) {
		return SpawnDensity.maxInstances((MobCategory) (Object) this, vanilla);
	}
}
