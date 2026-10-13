package dev.vadim.bcenchantments.vanilla;

import dev.vadim.bcenchantments.combat.CombatHooks;
import dev.vadim.bcenchantments.perk.Perks;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;

/**
 * Fire Aspect, reworked to build up instead of setting a full burn on the first hit. Each melee hit
 * adds one stack of burn, up to {@link #MAX_STACKS}, and every stack deals {@link #DAMAGE_PER_STACK}
 * a second. The burn lasts 4 seconds per Fire Aspect level after the last hit, and water or rain puts
 * it out. The target shows as burning, but vanilla's own afterburn damage is held off while it does,
 * so only the stacks hurt. Armor stands never catch fire from it. Vanilla's ignite effect is removed
 * by the {@code data/minecraft/enchantment/fire_aspect.json} override.
 */
public final class FireAspect {
	private static final int MAX_STACKS = 5;
	/** 0.2 hearts a second per stack. */
	private static final float DAMAGE_PER_STACK = 0.4F;
	private static final int SECONDS_PER_LEVEL = 4;
	private static final int TICKS_PER_SECOND = 20;

	private static final class Burn {
		int stacks;
		long until;
		long nextDamage;
		LivingEntity attacker;
	}

	private static final Map<LivingEntity, Burn> BURNING = new WeakHashMap<>();
	private static long now;

	private FireAspect() {
	}

	public static void init() {
		ServerLivingEntityEvents.AFTER_DAMAGE.register((victim, source, base, taken, blocked) -> {
			if (!blocked && taken > 0.0F) {
				onHit(victim, source);
			}
		});
		// Vanilla's afterburn has no attacker; the stacks' own damage does, so it gets through.
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((victim, source, amount) ->
			!(source.is(DamageTypes.ON_FIRE) && source.getEntity() == null && BURNING.containsKey(victim)));
		CombatHooks.onServerTick(FireAspect::tick);
	}

	private static void onHit(LivingEntity victim, DamageSource source) {
		if (!source.isDirect() || !(source.getEntity() instanceof LivingEntity attacker) || !melee(source)
			|| victim instanceof ArmorStand || victim.fireImmune()) {
			return;
		}
		ItemStack weapon = attacker.getWeaponItem();
		int level = weapon == null ? 0 : Perks.level(weapon, Enchantments.FIRE_ASPECT);
		if (level <= 0) {
			return;
		}
		Burn burn = BURNING.computeIfAbsent(victim, v -> {
			Burn fresh = new Burn();
			fresh.nextDamage = now + TICKS_PER_SECOND;
			return fresh;
		});
		burn.stacks = Math.min(burn.stacks + 1, MAX_STACKS);
		burn.until = Math.max(burn.until, now + (long) SECONDS_PER_LEVEL * level * TICKS_PER_SECOND);
		burn.attacker = attacker;
		victim.setRemainingFireTicks(Math.max(victim.getRemainingFireTicks(), (int) (burn.until - now)));
	}

	private static boolean melee(DamageSource source) {
		return source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.MOB_ATTACK) || source.is(DamageTypes.MOB_ATTACK_NO_AGGRO)
			|| source.is(DamageTypes.MACE_SMASH) || source.is(DamageTypes.SPEAR);
	}

	private static void tick() {
		now++;
		Iterator<Map.Entry<LivingEntity, Burn>> it = BURNING.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<LivingEntity, Burn> entry = it.next();
			LivingEntity victim = entry.getKey();
			Burn burn = entry.getValue();
			if (victim.isRemoved() || !victim.isAlive() || now > burn.until || victim.isInWaterOrRain()) {
				if (victim.isInWaterOrRain() || now > burn.until) {
					victim.clearFire();
				}
				it.remove();
				continue;
			}
			// Kept lit for the look, and for cooked drops.
			victim.setRemainingFireTicks(Math.max(victim.getRemainingFireTicks(), (int) (burn.until - now)));
			// A hit's hurt cooldown would swallow the burn, so it waits it out, like vanilla fire does.
			if (now >= burn.nextDamage && victim.invulnerableTime <= 10 && victim.level() instanceof ServerLevel world) {
				burn.nextDamage = now + TICKS_PER_SECOND;
				DamageSource fire = new DamageSource(
					world.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.ON_FIRE),
					null, burn.attacker.isRemoved() ? null : burn.attacker
				);
				victim.hurtServer(world, fire, DAMAGE_PER_STACK * burn.stacks);
			}
		}
	}
}
