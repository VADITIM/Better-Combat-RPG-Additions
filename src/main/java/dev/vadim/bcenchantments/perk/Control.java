package dev.vadim.bcenchantments.perk;

import dev.vadim.bcenchantments.RpgAdditions;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Crowd control shared by the perks: rooting holds an enemy in place, suspending also hangs it in the
 * air, stunned so it cannot strike. Both are transient, so nothing is left on an enemy that is saved
 * mid-effect.
 */
public final class Control {
	private static final Identifier ROOT = RpgAdditions.id("root");

	private static final class Hold {
		long until;
		/** When a suspended enemy stops rising and starts hanging; -1 when it is only rooted. */
		long hangFrom = -1;
	}

	private static final Map<LivingEntity, Hold> HELD = new WeakHashMap<>();
	private static long now;

	private Control() {
	}

	/** Holds {@code target} in place for {@code ticks}, or longer if it is already held. */
	static void root(LivingEntity target, int ticks) {
		hold(target, ticks);
	}

	/** Lets {@code target} rise for {@code riseTicks}, then hangs it in the air, stunned, for {@code hangTicks}. */
	static void suspend(LivingEntity target, int riseTicks, int hangTicks) {
		Hold hold = hold(target, riseTicks + hangTicks);
		hold.hangFrom = now + riseTicks;
	}

	private static Hold hold(LivingEntity target, int ticks) {
		Hold hold = HELD.computeIfAbsent(target, t -> new Hold());
		hold.until = Math.max(hold.until, now + ticks);
		AttributeInstance speed = target.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null) {
			speed.addOrUpdateTransientModifier(new AttributeModifier(ROOT, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		}
		return hold;
	}

	/** Whether {@code entity} hangs stunned in the air and so cannot attack. */
	public static boolean stunned(LivingEntity entity) {
		Hold hold = HELD.get(entity);
		return hold != null && hold.hangFrom >= 0 && now <= hold.until;
	}

	static void tick() {
		now++;
		Iterator<Map.Entry<LivingEntity, Hold>> it = HELD.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<LivingEntity, Hold> entry = it.next();
			LivingEntity target = entry.getKey();
			Hold hold = entry.getValue();
			if (target.isRemoved() || !target.isAlive() || now > hold.until) {
				AttributeInstance speed = target.getAttribute(Attributes.MOVEMENT_SPEED);
				if (speed != null) {
					speed.removeModifier(ROOT);
				}
				it.remove();
				continue;
			}
			if (target instanceof Mob mob) {
				mob.getNavigation().stop();
			}
			if (hold.hangFrom >= 0 && now >= hold.hangFrom) {
				// Moves by its motion before gravity applies, so zeroing it each tick keeps it where it is.
				target.setDeltaMovement(0.0, 0.0, 0.0);
				target.resetFallDistance();
			}
		}
	}
}
