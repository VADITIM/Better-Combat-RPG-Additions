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
 * several powers plays their effects one after another, each through once. The mace's Perfect Hit
 * window ({@link MaceTimingClient}) shows over all of them while it is open.
 *
 * <p>The mixins on the item render state carry the weapon's effect from model resolving to submission:
 * {@code ItemModelResolverMixin} picks it and marks the state animated so slots redraw every frame,
 * and {@code ItemStackRenderStateMixin} submits the extra quads after each layer.
 */
public final class PowerEffects {
	/** Frame counts must match {@code tools/power_effects.py}. */
	private enum Effect {
		LIGHTNING("lightning", 32, 70),
		SHOCKWAVE("shockwave", 20, 60),
		COLD_EDGE("cold_edge", 24, 90),
		UPPERCUT("uppercut", 16, 60),
		CHAINS("chains", 12, 90),
		ECHO("echo", 24, 70),
		WIND_CHARGED("wind_charged", 16, 70),
		GRAVITY("gravity", 24, 70),
		MOMENTUM("momentum", 16, 50),
		RAMPAGING("rampaging", 24, 60),
		/** The mace's Perfect Hit window; its frames follow the window, not the clock. */
		PERFECT_HIT("perfect_hit", 6, 25);

		final String id;
		final int frames;
		final long frameMs;
		final RenderType renderType;

		Effect(String id, int frames, long frameMs) {
			this.id = id;
			this.frames = frames;
			this.frameMs = frameMs;
			this.renderType = RenderType.create(TrimEnchantments.MOD_ID + "_power_" + id,
				RenderSetup.builder(RenderPipelines.GLINT)
					.withTexture("Sampler0", TrimEnchantments.id("textures/item/power/" + id + ".png"))
					.createRenderSetup());
		}

		/** How long one run of the animation lasts. */
		long length() {
			return this.frames * this.frameMs;
		}
	}

	/** One frame of one effect: what an item state carries from resolving to submission. */
	private record Shown(Effect effect, int frame) {
	}

	/** The effect of the item state submitting right now; submission runs on the render thread only. */
	public static @Nullable Object submitting;
	private static final Map<Refinement.Perk, Effect> BY_PERK = new EnumMap<>(Refinement.Perk.class);

	static {
		for (Effect effect : Effect.values()) {
			for (Refinement.Perk perk : Refinement.Perk.values()) {
				if (perk.id().equals(effect.id)) {
					BY_PERK.put(perk, effect);
				}
			}
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
		float window = MaceTimingClient.sparkle(stack);
		if (window >= 0.0F) {
			Effect perfect = Effect.PERFECT_HIT;
			int frame = Math.min(perfect.frames - 1, (int) (window * 50.0F / perfect.frameMs));
			return new Shown(perfect, frame);
		}
		if (!Refinement.isWeapon(stack)) {
			return null;
		}
		List<Effect> effects = new ArrayList<>();
		long total = 0;
		for (Refinement.Power power : Refinement.powersOf(stack)) {
			Refinement.Perk perk = Refinement.perk(power.trim());
			Effect effect = perk == null ? null : BY_PERK.get(perk);
			if (effect != null) {
				effects.add(effect);
				total += effect.length();
			}
		}
		if (effects.isEmpty()) {
			return null;
		}
		// Each effect plays its animation through once, then hands over to the next; none overlap or cut in.
		long time = Util.getMillis() % total;
		for (Effect effect : effects) {
			if (time < effect.length()) {
				return new Shown(effect, (int) (time / effect.frameMs));
			}
			time -= effect.length();
		}
		return null;
	}

	/**
	 * Copies of {@code quads} that draw the current frame of {@code effect}: UVs come from each vertex's
	 * position in the item model, so a texture pixel lands on exactly one item pixel.
	 */
	public static List<BakedQuad> quads(Object effect, List<BakedQuad> quads) {
		Shown shown = (Shown) effect;
		Effect current = shown.effect();
		int frame = shown.frame();
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
