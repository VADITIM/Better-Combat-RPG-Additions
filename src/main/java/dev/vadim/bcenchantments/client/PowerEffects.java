package dev.vadim.bcenchantments.client;

import dev.vadim.bcenchantments.TrimEnchantments;
import dev.vadim.bcenchantments.progression.Refinement;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

/**
 * The animation each trim power plays on its weapon, wherever the weapon is drawn: in slots, in hand,
 * dropped or in a frame. A frame from {@code textures/item/power/<power>.png} (made by
 * {@code tools/power_effects.py}) is laid 1:1 over the item's 16x16 sprite and drawn with the
 * enchantment-glint pipeline, whose equal-depth test keeps it on the item's own pixels. A weapon with
 * several powers cycles through their effects.
 *
 * <p>The mixins on the item render state carry the weapon's effect from model resolving to submission:
 * {@code ItemModelResolverMixin} picks it and marks the state animated so slots redraw every frame,
 * and {@code ItemStackRenderStateMixin} submits the extra quads after each layer.
 */
public final class PowerEffects {
	/** Frame counts must match {@code tools/power_effects.py}. */
	private enum Effect {
		LIGHTNING(Refinement.Perk.LIGHTNING, 32, 70),
		SHOCKWAVE(Refinement.Perk.SHOCKWAVE, 20, 60),
		COLD_EDGE(Refinement.Perk.COLD_EDGE, 24, 90),
		UPPERCUT(Refinement.Perk.UPPERCUT, 16, 60),
		CHAINS(Refinement.Perk.CHAINS, 12, 90),
		ECHO(Refinement.Perk.ECHO, 24, 70),
		WIND_CHARGED(Refinement.Perk.WIND_CHARGED, 16, 70),
		GRAVITY(Refinement.Perk.GRAVITY, 24, 70),
		MOMENTUM(Refinement.Perk.MOMENTUM, 16, 50),
		RAMPAGING(Refinement.Perk.RAMPAGING, 24, 60);

		final Refinement.Perk perk;
		final int frames;
		final long frameMs;
		final RenderType renderType;

		Effect(Refinement.Perk perk, int frames, long frameMs) {
			this.perk = perk;
			this.frames = frames;
			this.frameMs = frameMs;
			this.renderType = RenderType.create(TrimEnchantments.MOD_ID + "_power_" + perk.id(),
				RenderSetup.builder(RenderPipelines.GLINT)
					.withTexture("Sampler0", TrimEnchantments.id("textures/item/power/" + perk.id() + ".png"))
					.createRenderSetup());
		}
	}

	/** How long each power's effect shows before a weapon with several moves on to the next. */
	private static final long CYCLE_MS = 3000;
	/** The effect of the item state submitting right now; submission runs on the render thread only. */
	public static @Nullable Object submitting;
	private static final Map<Refinement.Perk, Effect> BY_PERK = new EnumMap<>(Refinement.Perk.class);

	static {
		for (Effect effect : Effect.values()) {
			BY_PERK.put(effect.perk, effect);
		}
	}

	/** Implemented by the item render state; holds the effect to draw for the stack it was resolved from. */
	public interface Holder {
		@Nullable Object bcenchantments$effect();

		void bcenchantments$setEffect(@Nullable Object effect);
	}

	private PowerEffects() {
	}

	/** The effect {@code stack} shows right now, or null for an item without a power that has one. */
	public static @Nullable Object effectFor(ItemStack stack) {
		if (!Refinement.isWeapon(stack)) {
			return null;
		}
		List<Effect> effects = new ArrayList<>();
		for (Refinement.Power power : Refinement.powersOf(stack)) {
			Refinement.Perk perk = Refinement.perk(power.trim());
			Effect effect = perk == null ? null : BY_PERK.get(perk);
			if (effect != null) {
				effects.add(effect);
			}
		}
		if (effects.isEmpty()) {
			return null;
		}
		return effects.get((int) (Util.getMillis() / CYCLE_MS % effects.size()));
	}

	/**
	 * Copies of {@code quads} that draw the current frame of {@code effect}: UVs come from each vertex's
	 * position in the item model, so a texture pixel lands on exactly one item pixel.
	 */
	public static List<BakedQuad> quads(Object effect, List<BakedQuad> quads) {
		Effect current = (Effect) effect;
		int frame = (int) (Util.getMillis() / current.frameMs % current.frames);
		List<BakedQuad> out = new ArrayList<>(quads.size());
		for (BakedQuad quad : quads) {
			BakedQuad.MaterialInfo material = quad.materialInfo();
			BakedQuad.MaterialInfo info = new BakedQuad.MaterialInfo(material.sprite(), material.layer(), current.renderType, -1, false, 0);
			out.add(new BakedQuad(quad.position0(), quad.position1(), quad.position2(), quad.position3(),
				uv(quad.position0(), frame, current.frames), uv(quad.position1(), frame, current.frames),
				uv(quad.position2(), frame, current.frames), uv(quad.position3(), frame, current.frames),
				quad.direction(), info));
		}
		return out;
	}

	/** Model x runs left to right and y bottom to top over the sprite; frames stack downward in the strip. */
	private static long uv(Vector3fc position, int frame, int frames) {
		float u = Mth.clamp(position.x(), 0.0F, 1.0F);
		float v = Mth.clamp(1.0F - position.y(), 0.0F, 0.9999F);
		return UVPair.pack(u, (frame + v) / frames);
	}
}
