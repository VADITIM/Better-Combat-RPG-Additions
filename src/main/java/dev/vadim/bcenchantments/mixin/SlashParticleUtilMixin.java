package dev.vadim.bcenchantments.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.vadim.bcenchantments.weapon.SpearStyle;
import net.bettercombat.api.fx.ParticlePlacement;
import net.bettercombat.client.particle.SlashParticleUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Keeps the spear's stab trail on the spear in first person, whatever the player's FOV.
 * Better Combat spawns a stab trail about 2.75 blocks ahead, offset sideways and down from a point
 * 0.25 below the eyes. The hand is drawn at a fixed 70 degree FOV but the trail at the player's own,
 * so offsets tuned at 70 drift toward the crosshair at a wider FOV. Scaling the offsets by the FOV
 * keeps the trail on the spear tip. Other players see the offsets as written in {@code spear.json}.
 */
@Mixin(SlashParticleUtil.class)
public abstract class SlashParticleUtilMixin {
	/** Better Combat's trail origin sits this far below the eyes. */
	private static final float ORIGIN_DROP = 0.25F;
	private static final double HAND_HALF_FOV = Math.toRadians(35.0);

	@WrapOperation(
		method = "spawnParticles(Lnet/minecraft/client/player/AbstractClientPlayer;ZFLjava/util/List;Lnet/bettercombat/api/fx/TrailAppearance;)V",
		at = @At(value = "INVOKE", target = "Lnet/bettercombat/api/fx/ParticlePlacement;x_addition()F")
	)
	private static float bcenchantments$scaleX(ParticlePlacement placement, Operation<Float> original, @Local(argsOnly = true) AbstractClientPlayer player) {
		return original.call(placement) * fovScale(player);
	}

	@WrapOperation(
		method = "spawnParticles(Lnet/minecraft/client/player/AbstractClientPlayer;ZFLjava/util/List;Lnet/bettercombat/api/fx/TrailAppearance;)V",
		at = @At(value = "INVOKE", target = "Lnet/bettercombat/api/fx/ParticlePlacement;y_addition()F")
	)
	private static float bcenchantments$scaleY(ParticlePlacement placement, Operation<Float> original, @Local(argsOnly = true) AbstractClientPlayer player) {
		// Scale the drop from the eyes, not from Better Combat's origin below them.
		return ORIGIN_DROP + (original.call(placement) - ORIGIN_DROP) * fovScale(player);
	}

	private static float fovScale(AbstractClientPlayer player) {
		Minecraft client = Minecraft.getInstance();
		if (player != client.player
			|| !client.options.getCameraType().isFirstPerson()
			|| !SpearStyle.INSTANCE.matches(player.getMainHandItem())) {
			return 1.0F;
		}
		double halfFov = Math.toRadians(client.options.fov().get() / 2.0);
		return (float) (Math.tan(halfFov) / Math.tan(HAND_HALF_FOV));
	}
}
