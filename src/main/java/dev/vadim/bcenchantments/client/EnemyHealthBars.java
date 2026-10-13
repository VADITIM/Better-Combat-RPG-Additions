package dev.vadim.bcenchantments.client;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

/**
 * A health bar beside every hurt enemy, pinned to the right of its head as seen from the camera, so it
 * follows the mob through the world. The bar stands upright in a dark hairline frame, red filling from
 * the bottom. Damage taken within a combo stays above the red as a white trail: the last hit bright white, the hits before
 * it a softer white. Once the mob stops being hit and the player's combo ends, the trail drains down
 * to the red. Each hit's damage rises out of a random point inside the mob, fixed in the world and
 * projected like the bar. Bars arrive slowly and leave fast, like the rest of the {@link Theme} HUD,
 * except on death, when they go at once.
 */
public final class EnemyHealthBars implements HudElement {
	private static final double RANGE = 12.0;
	/** Distance at which bars start to fade, gone by {@link #RANGE}: clear up close, gone at range. */
	private static final double FADE_FROM = 5.0;
	/** Gap between the mob's side and the bar, in blocks. */
	private static final double SIDE_GAP = 0.2;
	private static final int MAX_BARS = 32;

	/** Inside of the upright bar, in GUI pixels; the frame adds one on each side. */
	private static final int BAR_WIDTH = 2;
	private static final int BAR_HEIGHT = 16;

	/** How long a hit keeps the combo trail up when the player's own combo is not running. */
	private static final long HOLD_MS = 450;
	/** How long the trail takes to drain once the combo is over. */
	private static final long DRAIN_MS = 160;
	/** How long a damage number floats up out of the mob. */
	private static final long NUMBER_MS = 700;
	/** How far a damage number rises over its life, in blocks. */
	private static final double NUMBER_RISE = 0.5;
	private static final int MAX_NUMBERS = 48;
	private static final long HIT_FLASH_MS = 180;
	/** How long an enemy that is no longer hurt or targeted keeps its bar. */
	private static final long LINGER_MS = 2500;

	/** One enemy's bar and the damage it has taken in the current combo. */
	private static final class Bar {
		float health;
		float maxHealth;
		/** Health before the combo's first hit: the trail runs from here down to {@link #health}. */
		float comboFrom;
		/** Health before the last hit, inside the trail. */
		float lastFrom;
		long lastHitAt = -1;
		long drainAt = -1;
		float drainFrom;
		boolean visible;
		long enterAt = -1;
		long leaveAt = -1;
		long wantedAt = -1;
		boolean seen;
		boolean sighted;

		Bar(LivingEntity mob) {
			health = mob.getHealth();
			maxHealth = mob.getMaxHealth();
			comboFrom = health;
			lastFrom = health;
		}

		/** Reads the mob's health and returns the damage taken since the last tick, or 0. */
		float tick(LivingEntity mob, long now) {
			float before = health;
			health = Math.max(0.0F, mob.getHealth());
			maxHealth = Math.max(1.0F, mob.getMaxHealth());
			if (health < before - 0.001F) {
				if (!comboRunning(now)) {
					comboFrom = trail(now);
					drainAt = -1;
				}
				lastFrom = before;
				lastHitAt = now;
			} else if (health > before) {
				// Healing eats into the trail from below, never past its top.
				comboFrom = Math.max(comboFrom, health);
				lastFrom = Math.max(lastFrom, health);
			}
			if (drainAt < 0 && comboFrom > health && !comboRunning(now)) {
				drainAt = now;
				drainFrom = comboFrom;
			}
			return Math.max(0.0F, before - health);
		}

		boolean comboRunning(long now) {
			return lastHitAt >= 0 && (now - lastHitAt < HOLD_MS || ComboHud.comboActive());
		}

		/** Top of the white trail, draining toward the red once the combo is over. */
		float trail(long now) {
			if (drainAt < 0) {
				return Math.max(comboFrom, health);
			}
			float t = Theme.easeIn((now - drainAt) / (float) DRAIN_MS);
			float top = Mth.lerp(t, drainFrom, health);
			if (t >= 1.0F) {
				comboFrom = health;
				lastFrom = health;
				drainAt = -1;
			}
			return top;
		}

		boolean update(boolean wanted, long now) {
			if (wanted && !visible) {
				visible = true;
				enterAt = now;
			} else if (!wanted && visible) {
				visible = false;
				leaveAt = now;
			}
			return visible || leaveAt >= 0 && now - leaveAt < Theme.LEAVE_MS;
		}
	}

	private record Draw(Bar bar, double x, double y, double distance) {
	}

	/** One hit's damage, fixed in the world where it left the mob. */
	private record DamageNumber(Vec3 from, String text, long at) {
	}

	private static final Int2ObjectOpenHashMap<Bar> BARS = new Int2ObjectOpenHashMap<>();
	private static final List<DamageNumber> NUMBERS = new ArrayList<>();

	/** Reads every nearby enemy's health once a tick, and whether the player can see it. */
	public static void tick(Minecraft mc) {
		if (mc.level == null || mc.player == null) {
			BARS.clear();
			NUMBERS.clear();
			return;
		}
		long now = Util.getMillis();
		NUMBERS.removeIf(number -> now - number.at() >= NUMBER_MS);
		for (Bar bar : BARS.values()) {
			bar.seen = false;
		}
		int sighted = 0;
		for (Entity entity : mc.level.entitiesForRendering()) {
			if (!(entity instanceof LivingEntity mob) || !(entity instanceof Enemy) || entity.isRemoved()
				|| mob.isInvisible() || mob.distanceToSqr(mc.player) > RANGE * RANGE) {
				continue;
			}
			Bar bar = BARS.get(mob.getId());
			if (bar == null) {
				bar = new Bar(mob);
				BARS.put(mob.getId(), bar);
			}
			float damage = bar.tick(mob, now);
			if (damage >= 0.05F && NUMBERS.size() < MAX_NUMBERS && mc.player.hasLineOfSight(mob)) {
				NUMBERS.add(new DamageNumber(inside(mob), amount(damage), now));
			}
			// A dead mob's bar goes at once, with no leave.
			if (mob.isDeadOrDying()) {
				continue;
			}
			bar.seen = true;
			boolean hurt = bar.health < bar.maxHealth - 0.001F || bar.comboFrom > bar.health;
			if (hurt || mc.crosshairPickEntity == mob) {
				bar.wantedAt = now;
			}
			// Line of sight is a ray cast, so only enemies that would show a bar pay for it.
			bar.sighted = bar.wantedAt >= 0 && now - bar.wantedAt < LINGER_MS && sighted < MAX_BARS && mc.player.hasLineOfSight(mob);
			if (bar.sighted) {
				sighted++;
			}
		}
		BARS.values().removeIf(bar -> !bar.seen);
	}

	/** A random point inside the mob's middle, where a damage number starts. */
	private static Vec3 inside(LivingEntity mob) {
		ThreadLocalRandom random = ThreadLocalRandom.current();
		double half = mob.getBbWidth() * 0.3;
		double height = mob.getBbHeight();
		return mob.position().add(random.nextDouble(-half, half), random.nextDouble(height * 0.35, height * 0.8), random.nextDouble(-half, half));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null || BARS.isEmpty() && NUMBERS.isEmpty()) {
			return;
		}
		long now = Util.getMillis();
		float partial = delta.getGameTimeDeltaPartialTick(false);
		Camera camera = mc.gameRenderer.mainCamera();
		Vec3 eye = camera.position();
		Vector3fc forward = camera.forwardVector();
		Vector3fc left = camera.leftVector();

		List<Draw> draws = new ArrayList<>();
		for (Iterator<Int2ObjectOpenHashMap.Entry<Bar>> it = BARS.int2ObjectEntrySet().fastIterator(); it.hasNext();) {
			Int2ObjectOpenHashMap.Entry<Bar> entry = it.next();
			Bar bar = entry.getValue();
			Entity entity = mc.level.getEntity(entry.getIntKey());
			if (!bar.update(bar.sighted, now) || entity == null || entity instanceof LivingEntity mob && mob.isDeadOrDying()) {
				continue;
			}
			// The right of the mob as the camera sees it, level with its eyes.
			double side = entity.getBbWidth() / 2.0 + SIDE_GAP;
			Vec3 anchor = entity.getPosition(partial).add(-left.x() * side, entity.getEyeHeight(), -left.z() * side);
			Vec3 toAnchor = anchor.subtract(eye);
			if (toAnchor.x * forward.x() + toAnchor.y * forward.y() + toAnchor.z * forward.z() < 0.1) {
				continue;
			}
			Vec3 ndc = mc.gameRenderer.projectPointToScreen(anchor);
			if (Math.abs(ndc.x) > 1.2 || Math.abs(ndc.y) > 1.2) {
				continue;
			}
			double x = (ndc.x + 1.0) / 2.0 * g.guiWidth();
			double y = (1.0 - ndc.y) / 2.0 * g.guiHeight();
			draws.add(new Draw(bar, x, y, toAnchor.length()));
		}
		// Far bars first, so nearer ones sit on top.
		draws.sort((a, b) -> Double.compare(b.distance(), a.distance()));
		for (Draw draw : draws) {
			draw(g, mc.font, draw, now);
		}

		// Damage numbers leave the mob from inside it and rise through the world, projected like the bars.
		for (DamageNumber number : NUMBERS) {
			float t = Mth.clamp((now - number.at()) / (float) NUMBER_MS, 0.0F, 1.0F);
			Vec3 at = number.from().add(0.0, NUMBER_RISE * Theme.easeOut(t), 0.0);
			Vec3 toAt = at.subtract(eye);
			if (toAt.x * forward.x() + toAt.y * forward.y() + toAt.z * forward.z() < 0.1) {
				continue;
			}
			float fade = 1.0F - (float) Mth.clamp((toAt.length() - FADE_FROM) / (RANGE - FADE_FROM), 0.0, 1.0);
			// Full until the last third, then gone fast.
			float alpha = fade * (1.0F - Theme.easeIn(Mth.clamp((t - 0.65F) / 0.35F, 0.0F, 1.0F)));
			if (alpha <= 0.02F) {
				continue;
			}
			Vec3 ndc = mc.gameRenderer.projectPointToScreen(at);
			if (Math.abs(ndc.x) > 1.1 || Math.abs(ndc.y) > 1.1) {
				continue;
			}
			int x = (int) Math.round((ndc.x + 1.0) / 2.0 * g.guiWidth());
			int y = (int) Math.round((1.0 - ndc.y) / 2.0 * g.guiHeight());
			g.text(mc.font, number.text(), x - mc.font.width(number.text()) / 2, y - 4, Theme.argb(alpha, Theme.TEXT_BRIGHT), true);
		}
	}

	private static void draw(GuiGraphicsExtractor g, Font font, Draw draw, long now) {
		Bar bar = draw.bar();
		float enter = bar.visible ? Theme.easeOut((now - bar.enterAt) / (float) Theme.ENTER_MS) : 1.0F;
		float leave = bar.visible ? 0.0F : Theme.easeIn((now - bar.leaveAt) / (float) Theme.LEAVE_MS);
		float fade = 1.0F - (float) Mth.clamp((draw.distance() - FADE_FROM) / (RANGE - FADE_FROM), 0.0, 1.0);
		float alpha = enter * (1.0F - leave) * fade;
		if (alpha <= 0.02F) {
			return;
		}
		// Arrives sliding out from the mob, leaves sliding back into it.
		int x0 = (int) Math.round(draw.x() - (1.0F - enter) * 3.0F - leave * 3.0F);
		int y = (int) Math.round(draw.y());
		int x1 = x0 + BAR_WIDTH + 2;
		int y0 = y - BAR_HEIGHT / 2 - 1;
		int y1 = y0 + BAR_HEIGHT + 2;
		Theme.box(g, x0, y0, x1, y1, Theme.argb(alpha * Theme.PANEL_ALPHA, Theme.SUNKEN));
		Theme.hairline(g, x0, y0, x1, y1, Theme.argb(alpha, Theme.BORDER_CONTROL));

		// Fills from the bottom up: red health, then the combo's white trail above it.
		int left = x0 + 1;
		int right = x1 - 1;
		int bottom = y1 - 1;
		float trail = bar.trail(now);
		int healthY = bottom - pixels(bar.health, bar.maxHealth);
		int trailY = bottom - pixels(trail, bar.maxHealth);
		int lastY = Math.max(trailY, bottom - pixels(bar.lastFrom, bar.maxHealth));

		// The combo's earlier hits a softer white, the last one bright, flashing as it lands.
		if (lastY > trailY) {
			g.fill(left, trailY, right, lastY, Theme.argb(alpha * 0.55F, Theme.TEXT_BODY));
		}
		if (healthY > lastY) {
			float flash = bar.lastHitAt >= 0 ? 1.0F - Mth.clamp((now - bar.lastHitAt) / (float) HIT_FLASH_MS, 0.0F, 1.0F) : 0.0F;
			g.fill(left, lastY, right, healthY, Theme.argb(alpha * (0.85F + 0.15F * flash), 0xFFFFFF));
		}
		if (bottom > healthY) {
			g.fill(left, healthY, right, bottom, Theme.argb(alpha, Theme.HEALTH));
			// A lighter left column, the bevel vanilla's own bars carry, turned upright.
			g.fill(left, healthY, left + 1, bottom, Theme.argb(alpha, Theme.HEALTH_LIGHT));
		}
	}

	private static int pixels(float health, float maxHealth) {
		return Mth.clamp(Math.round(health / maxHealth * BAR_HEIGHT), 0, BAR_HEIGHT);
	}

	/** Damage in health points, one decimal at most. */
	private static String amount(float damage) {
		float rounded = Math.round(damage * 10.0F) / 10.0F;
		return rounded == Math.round(rounded) ? String.valueOf(Math.round(rounded)) : String.format(Locale.ROOT, "%.1f", rounded);
	}
}
