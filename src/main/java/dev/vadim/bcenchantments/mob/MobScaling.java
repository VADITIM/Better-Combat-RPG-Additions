package dev.vadim.bcenchantments.mob;

import dev.vadim.bcenchantments.RpgAdditions;
import dev.vadim.bcenchantments.perk.Perks;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;

/**
 * Hidden stat buffs for monsters. Health and damage are rolled per monster; armor and knockback
 * resistance scale with local difficulty and with how strong the nearby players' weapons are, so
 * powered weapons meet sturdier enemies instead of trivialising them. A monster is
 * scaled once, the first time it joins the world; the modifiers are permanent, so they are saved with
 * it and mark it as done. Fall damage grows with the added health, so mob farms still kill at the
 * same drop height.
 */
public final class MobScaling {
	private static final Identifier HEALTH = RpgAdditions.id("mob_health");
	private static final Identifier DAMAGE = RpgAdditions.id("mob_damage");
	private static final Identifier ARMOR = RpgAdditions.id("mob_armor");
	private static final Identifier KNOCKBACK_RESISTANCE = RpgAdditions.id("mob_knockback_resistance");

	private MobScaling() {
	}

	public static void init() {
		ServerEntityEvents.ENTITY_LOAD.register(MobScaling::onLoad);
	}

	private static void onLoad(Entity entity, ServerLevel world) {
		if (!(entity instanceof Mob mob) || !(entity instanceof Enemy)) {
			return;
		}
		AttributeInstance maxHealth = mob.getAttribute(Attributes.MAX_HEALTH);
		if (maxHealth == null || maxHealth.hasModifier(HEALTH)) {
			return;
		}
		float difficulty = world.getCurrentDifficultyAt(mob.blockPosition()).getSpecialMultiplier();
		int power = Math.min(nearbyPower(world, mob), MobBalance.MAX_POWER);

		float healthShare = mob.getHealth() / mob.getMaxHealth();
		add(mob, Attributes.MAX_HEALTH, HEALTH, AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
			MobBalance.HEALTH_MAX * bell(mob.getRandom()));
		add(mob, Attributes.ATTACK_DAMAGE, DAMAGE, AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
			MobBalance.DAMAGE_MAX * mob.getRandom().nextDouble());
		add(mob, Attributes.ARMOR, ARMOR, AttributeModifier.Operation.ADD_VALUE,
			MobBalance.ARMOR_BASE + MobBalance.ARMOR_PER_DIFFICULTY * difficulty + MobBalance.ARMOR_PER_POWER * power);
		add(mob, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_RESISTANCE, AttributeModifier.Operation.ADD_VALUE,
			MobBalance.KNOCKBACK_RESISTANCE_BASE + MobBalance.KNOCKBACK_RESISTANCE_PER_DIFFICULTY * difficulty
				+ MobBalance.KNOCKBACK_RESISTANCE_PER_POWER * power);
		// Keeps the share of health it had, so a fresh spawn starts full and a hurt one stays as hurt.
		mob.setHealth(healthShare * mob.getMaxHealth());
	}

	/** A roll in [0, 1] that lands near the middle most often: the mean of three even rolls. */
	private static double bell(RandomSource random) {
		return (random.nextDouble() + random.nextDouble() + random.nextDouble()) / 3.0;
	}

	/** Fall damage on a scaled monster, raised by as much as its health was, so it dies from the same height as before. */
	public static float fallDamage(LivingEntity entity, DamageSource source, float amount) {
		if (!source.is(DamageTypeTags.IS_FALL)) {
			return amount;
		}
		AttributeInstance maxHealth = entity.getAttribute(Attributes.MAX_HEALTH);
		AttributeModifier added = maxHealth == null ? null : maxHealth.getModifier(HEALTH);
		if (added == null) {
			return amount;
		}
		double scaled = maxHealth.getValue();
		double original = scaled - maxHealth.getBaseValue() * added.amount();
		return original > 0.0 ? (float) (amount * scaled / original) : amount;
	}

	private static void add(Mob mob, Holder<Attribute> attribute, Identifier id, AttributeModifier.Operation operation, double amount) {
		AttributeInstance instance = mob.getAttribute(attribute);
		if (instance != null && amount != 0.0) {
			instance.addOrReplacePermanentModifier(new AttributeModifier(id, amount, operation));
		}
	}

	/** The highest weapon power among players near {@code mob}: the perk levels on what they hold in either hand. */
	private static int nearbyPower(ServerLevel world, Mob mob) {
		int power = 0;
		double radiusSqr = MobBalance.POWER_RADIUS * MobBalance.POWER_RADIUS;
		for (ServerPlayer player : world.players()) {
			if (!player.isSpectator() && player.distanceToSqr(mob) <= radiusSqr) {
				power = Math.max(power, Perks.totalLevel(player.getMainHandItem()) + Perks.totalLevel(player.getOffhandItem()));
			}
		}
		return power;
	}
}
