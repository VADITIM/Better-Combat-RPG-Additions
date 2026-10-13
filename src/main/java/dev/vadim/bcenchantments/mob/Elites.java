package dev.vadim.bcenchantments.mob;

import dev.vadim.bcenchantments.TrimEnchantments;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * Elite monsters and captains. An elite carries one affix whose hits apply a status instead of more
 * damage; a captain is an elite that wears an ominous banner and hurries and shields the monsters
 * around it, so it is the one to kill first. Both are marked with entity tags, which save with the
 * monster, and get a little extra health folded into {@link MobScaling}'s roll.
 */
public final class Elites {
	enum Affix {
		/** Hits slow the player. */
		FROST(ParticleTypes.SNOWFLAKE),
		/** Hits weaken the player. */
		WEAKENING(ParticleTypes.SMOKE),
		/** Hits strip armor for a while, stacking. */
		SHREDDING(ParticleTypes.CRIT);

		final SimpleParticleType particle;

		Affix(SimpleParticleType particle) {
			this.particle = particle;
		}

		String id() {
			return name().toLowerCase();
		}

		String tag() {
			return TrimEnchantments.MOD_ID + ".elite." + id();
		}
	}

	private static final String CAPTAIN = TrimEnchantments.MOD_ID + ".captain";
	private static final Identifier SHRED = TrimEnchantments.id("elite_shred");
	private static final Set<Mob> LOADED = Collections.newSetFromMap(new WeakHashMap<>());
	private static final Map<ServerPlayer, Shred> SHREDDED = new WeakHashMap<>();

	private record Shred(int stacks, long until) {
	}

	private Elites() {
	}

	static void init() {
		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
			if (entity instanceof Mob mob && affix(mob) != null) {
				LOADED.add(mob);
			}
		});
		ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> LOADED.remove(entity));
		ServerTickEvents.END_SERVER_TICK.register(Elites::tick);
		ServerLivingEntityEvents.AFTER_DAMAGE.register(Elites::afterDamage);
		ServerLivingEntityEvents.AFTER_DEATH.register(Elites::onDeath);
	}

	/**
	 * Maybe makes a freshly spawned monster an elite or a captain, with {@code chance} the elite odds
	 * (captain odds are a share of it). Returns the max health share to add for its rank.
	 */
	static double promote(ServerLevel world, Mob mob, double chance) {
		if (mob instanceof WitherBoss || mob instanceof EnderDragon) {
			return 0.0;
		}
		boolean captain = canLead(mob) && mob.getRandom().nextDouble() < chance * MobBalance.CAPTAIN_SHARE;
		if (!captain && mob.getRandom().nextDouble() >= chance) {
			return 0.0;
		}
		Affix affix = Affix.values()[mob.getRandom().nextInt(Affix.values().length)];
		mob.addTag(affix.tag());
		Component name = Component.translatable("mob.bcenchantments.elite." + affix.id(), mob.getType().getDescription());
		if (captain) {
			mob.addTag(CAPTAIN);
			mob.setItemSlot(EquipmentSlot.HEAD, Raid.getOminousBannerInstance(world.registryAccess().lookupOrThrow(Registries.BANNER_PATTERN)));
			mob.setDropChance(EquipmentSlot.HEAD, 0.0F);
			name = Component.translatable("mob.bcenchantments.captain", name);
		}
		if (!mob.hasCustomName()) {
			mob.setCustomName(name);
		}
		LOADED.add(mob);
		return captain ? MobBalance.CAPTAIN_HEALTH : MobBalance.ELITE_HEALTH;
	}

	/** Monsters that show a banner on the head; the rest can be elites but not captains. Illagers have their own captains. */
	private static boolean canLead(Mob mob) {
		return mob instanceof Zombie || mob instanceof AbstractSkeleton || mob instanceof AbstractPiglin;
	}

	private static @Nullable Affix affix(Entity entity) {
		for (Affix affix : Affix.values()) {
			if (entity.entityTags().contains(affix.tag())) {
				return affix;
			}
		}
		return null;
	}

	private static void tick(MinecraftServer server) {
		long time = server.overworld().getGameTime();
		SHREDDED.entrySet().removeIf(entry -> {
			if (time < entry.getValue().until() && entry.getKey().isAlive()) {
				return false;
			}
			AttributeInstance armor = entry.getKey().getAttribute(Attributes.ARMOR);
			if (armor != null) {
				armor.removeModifier(SHRED);
			}
			return true;
		});
		if (time % MobBalance.ELITE_PULSE_TICKS != 0) {
			return;
		}
		for (Mob mob : new ArrayList<>(LOADED)) {
			if (!mob.isAlive() || !(mob.level() instanceof ServerLevel world)) {
				continue;
			}
			Affix affix = affix(mob);
			if (affix != null) {
				world.sendParticles(affix.particle, mob.getX(), mob.getY() + mob.getBbHeight() * 0.6, mob.getZ(), 2, 0.25, 0.35, 0.25, 0.0);
			}
			if (mob.entityTags().contains(CAPTAIN)) {
				rally(world, mob);
			}
		}
	}

	/** A captain hurries and shields the monsters around it for a little longer than a pulse. */
	private static void rally(ServerLevel world, Mob captain) {
		AABB area = captain.getBoundingBox().inflate(MobBalance.CAPTAIN_RADIUS);
		for (Mob follower : world.getEntitiesOfClass(Mob.class, area, other -> other != captain && other instanceof Enemy && other.isAlive())) {
			follower.addEffect(new MobEffectInstance(MobEffects.SPEED, MobBalance.CAPTAIN_EFFECT_TICKS, 0, true, false));
			follower.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, MobBalance.CAPTAIN_EFFECT_TICKS, 0, true, false));
		}
	}

	private static void afterDamage(LivingEntity victim, DamageSource source, float base, float taken, boolean blocked) {
		if (blocked || taken <= 0.0F || !(victim instanceof ServerPlayer player) || !(source.getEntity() instanceof Mob attacker)) {
			return;
		}
		Affix affix = affix(attacker);
		if (affix == null) {
			return;
		}
		switch (affix) {
			case FROST -> player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, MobBalance.FROST_TICKS, 1), attacker);
			case WEAKENING -> player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, MobBalance.WEAKENING_TICKS, 0), attacker);
			case SHREDDING -> shred(player);
		}
	}

	/** Strips armor, one stack per hit up to the cap; it all comes back once no hit lands for a while. */
	private static void shred(ServerPlayer player) {
		AttributeInstance armor = player.getAttribute(Attributes.ARMOR);
		if (armor == null) {
			return;
		}
		Shred old = SHREDDED.get(player);
		int stacks = Math.min((old == null ? 0 : old.stacks()) + 1, MobBalance.SHRED_MAX_STACKS);
		SHREDDED.put(player, new Shred(stacks, player.level().getServer().overworld().getGameTime() + MobBalance.SHRED_TICKS));
		armor.addOrUpdateTransientModifier(new AttributeModifier(SHRED, -MobBalance.SHRED_ARMOR * stacks, AttributeModifier.Operation.ADD_VALUE));
	}

	private static void onDeath(LivingEntity entity, DamageSource source) {
		if (!(entity instanceof Mob mob) || !(source.getEntity() instanceof ServerPlayer) || !(entity.level() instanceof ServerLevel world)
			|| affix(mob) == null) {
			return;
		}
		double chance = mob.entityTags().contains(CAPTAIN) ? MobBalance.CAPTAIN_EMERALD : MobBalance.ELITE_EMERALD;
		if (mob.getRandom().nextDouble() < chance) {
			mob.spawnAtLocation(world, Items.EMERALD);
		}
	}
}
