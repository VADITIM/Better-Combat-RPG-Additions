package dev.vadim.bcenchantments.perk;

import dev.vadim.bcenchantments.Particles;
import dev.vadim.bcenchantments.TrimEnchantments;
import dev.vadim.bcenchantments.combat.SwingResult;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Hits chill their targets for a moment. On a sweeping weapon (a sword) everything the swing catches
 * is chilled, on anything else only the direct hit. A target kept chilled for three seconds freezes
 * like it stood in powder snow until the chill runs out.
 */
final class ColdEdge {
	/** Movement speed taken off for levels 1 to 3. */
	private static final float[] SLOW = {0.10F, 0.25F, 0.40F};
	private static final int DURATION_TICKS = 25;
	private static final int FREEZE_AFTER_TICKS = 60;
	private static final Identifier MODIFIER = TrimEnchantments.id("cold_edge");
	private static final TagKey<Item> SWEEPING = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("minecraft", "enchantable/sweeping"));

	private static final class Chill {
		int level;
		long since;
		long until;
	}

	private static final Map<LivingEntity, Chill> CHILLED = new WeakHashMap<>();
	private static long now;

	private ColdEdge() {
	}

	static void onSwing(SwingResult swing) {
		int level = Perks.level(swing.weapon(), Perks.COLD_EDGE);
		if (level <= 0) {
			return;
		}
		if (swing.weapon().is(SWEEPING)) {
			for (LivingEntity hit : swing.hits()) {
				chill(hit, level);
			}
		} else {
			chill(swing.primary(), level);
		}
	}

	static void onDirectHit(ServerPlayer player, ItemStack weapon, LivingEntity target) {
		int level = Perks.level(weapon, Perks.COLD_EDGE);
		if (level > 0) {
			chill(target, level);
		}
	}

	static void chill(LivingEntity target, int level) {
		if (!target.isAlive()) {
			return;
		}
		level = Math.min(level, SLOW.length);
		Chill chill = CHILLED.get(target);
		if (chill == null) {
			chill = new Chill();
			chill.since = now;
			CHILLED.put(target, chill);
		}
		chill.level = Math.max(chill.level, level);
		chill.until = now + DURATION_TICKS;

		AttributeInstance speed = target.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null) {
			speed.addOrUpdateTransientModifier(new AttributeModifier(MODIFIER, -SLOW[chill.level - 1], AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		}
		if (target.level() instanceof ServerLevel world) {
			Particles.send(world, ParticleTypes.ITEM_SNOWBALL, target.getX(), target.getY(0.6), target.getZ(), 6, target.getBbWidth() * 0.4, target.getBbHeight() * 0.3, target.getBbWidth() * 0.4, 0.05);
			world.playSound((Entity) null, target.getX(), target.getY(), target.getZ(), SoundEvents.POWDER_SNOW_HIT, SoundSource.PLAYERS, 0.6F, 1.4F);
		}
	}

	/** Whether Cold Edge is slowing {@code target} right now. */
	static boolean chilled(LivingEntity target) {
		return CHILLED.containsKey(target);
	}

	static void tick() {
		now++;
		Iterator<Map.Entry<LivingEntity, Chill>> it = CHILLED.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<LivingEntity, Chill> entry = it.next();
			LivingEntity target = entry.getKey();
			Chill chill = entry.getValue();
			if (target.isRemoved() || !target.isAlive() || now > chill.until) {
				AttributeInstance speed = target.getAttribute(Attributes.MOVEMENT_SPEED);
				if (speed != null) {
					speed.removeModifier(MODIFIER);
				}
				it.remove();
				continue;
			}
			if (now - chill.since >= FREEZE_AFTER_TICKS && target.canFreeze()) {
				// Vanilla thaws by 2 a tick outside powder snow; staying 2 over the limit keeps it fully frozen,
				// so it takes freeze damage every 2 seconds, and lets it thaw on its own once the chill ends.
				target.setTicksFrozen(Math.max(target.getTicksFrozen(), target.getTicksRequiredToFreeze() + 2));
			}
			if (now % 4 == 0 && target.level() instanceof ServerLevel world) {
				Particles.send(world, ParticleTypes.SNOWFLAKE, target.getX(), target.getY(0.6), target.getZ(), 1, target.getBbWidth() * 0.4, target.getBbHeight() * 0.3, target.getBbWidth() * 0.4, 0.01);
			}
		}
	}
}
