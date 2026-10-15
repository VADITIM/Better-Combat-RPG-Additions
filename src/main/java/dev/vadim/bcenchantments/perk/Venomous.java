package dev.vadim.bcenchantments.perk;

import dev.vadim.bcenchantments.Particles;
import dev.vadim.bcenchantments.combat.SwingResult;
import dev.vadim.bcenchantments.mob.Adaptation;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * A hit throws a splash of poison straight up off its target, at most once every 8 s. Where it comes
 * down, the enemies around get Poison II; if it lands on the player it purifies them instead, clearing
 * every harmful effect. With {@link Synergy#TOXIC_STORM} every bolt of the weapon's lightning leaves
 * such a splash where it strikes.
 */
final class Venomous {
	/** Poison duration for levels 1 to 3: 2, 3 and 4 s. */
	private static final int[] POISON_TICKS = {40, 60, 80};
	private static final int POISON_AMPLIFIER = 1;
	private static final int COOLDOWN_TICKS = 160;
	private static final double RADIUS = 2.5;
	/** The throw: upward speed, sideways scatter, gravity and the longest it may fly. */
	private static final double THROW_UP = 0.55;
	private static final double THROW_SCATTER = 0.08;
	private static final double FALL = 0.05;
	private static final int MAX_FLIGHT = 40;
	private static final ColorParticleOption POISON = ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0.53F, 0.66F, 0.24F);

	private static final class Lob {
		final ServerPlayer player;
		final int level;
		Vec3 pos;
		Vec3 motion;
		int age;

		Lob(ServerPlayer player, int level, Vec3 pos, Vec3 motion) {
			this.player = player;
			this.level = level;
			this.pos = pos;
			this.motion = motion;
		}
	}

	private static final List<Lob> LOBS = new ArrayList<>();
	private static final Map<ServerPlayer, Integer> READY_AT = new WeakHashMap<>();

	private Venomous() {
	}

	static void onSwing(SwingResult swing) {
		toss(swing.player(), swing.weapon(), swing.primary());
	}

	static void onDirectHit(ServerPlayer player, ItemStack weapon, LivingEntity target) {
		toss(player, weapon, target);
	}

	private static void toss(ServerPlayer player, ItemStack weapon, LivingEntity target) {
		int level = level(weapon);
		Integer ready = READY_AT.get(player);
		if (level <= 0 || ready != null && player.tickCount < ready && player.tickCount >= ready - COOLDOWN_TICKS) {
			return;
		}
		READY_AT.put(player, player.tickCount + COOLDOWN_TICKS);
		Adaptation.used(player, weapon, Perks.VENOMOUS);
		double x = (player.getRandom().nextDouble() - 0.5) * 2.0 * THROW_SCATTER;
		double z = (player.getRandom().nextDouble() - 0.5) * 2.0 * THROW_SCATTER;
		LOBS.add(new Lob(player, level, target.position().add(0.0, target.getBbHeight(), 0.0), new Vec3(x, THROW_UP, z)));
		player.level().playSound((Entity) null, target.getX(), target.getY(), target.getZ(), SoundEvents.SPLASH_POTION_THROW, SoundSource.PLAYERS, 0.7F, 0.9F);
	}

	private static int level(ItemStack weapon) {
		return Math.min(Perks.level(weapon, Perks.VENOMOUS), POISON_TICKS.length);
	}

	static void tick() {
		Iterator<Lob> it = LOBS.iterator();
		while (it.hasNext()) {
			Lob lob = it.next();
			if (lob.player.isRemoved()) {
				it.remove();
				continue;
			}
			ServerLevel world = lob.player.level();
			lob.pos = lob.pos.add(lob.motion);
			lob.motion = new Vec3(lob.motion.x, lob.motion.y - FALL, lob.motion.z);
			lob.age++;
			Particles.send(world, POISON, lob.pos.x, lob.pos.y, lob.pos.z, 2, 0.05, 0.05, 0.05, 0.0);
			BlockPos at = BlockPos.containing(lob.pos);
			boolean landed = lob.motion.y < 0.0 && !world.getBlockState(at).getCollisionShape(world, at).isEmpty();
			if (landed || lob.age >= MAX_FLIGHT) {
				splash(lob.player, lob.pos, lob.level);
				it.remove();
			}
		}
	}

	/** A splash of poison at {@code at}: Poison II on the enemies around, a purifying one on the player. */
	static void splash(ServerPlayer player, Vec3 at, int level) {
		level = Math.clamp(level, 1, POISON_TICKS.length);
		ServerLevel world = player.level();
		for (LivingEntity enemy : Targets.enemiesNear(player, at, RADIUS, null)) {
			if (!Adaptation.resists(enemy, Perks.VENOMOUS)) {
				enemy.addEffect(new MobEffectInstance(MobEffects.POISON, POISON_TICKS[level - 1], POISON_AMPLIFIER), player);
			}
		}
		if (player.distanceToSqr(at) <= RADIUS * RADIUS) {
			purify(player);
		}
		Particles.send(world, POISON, at.x, at.y + 0.2, at.z, 40, RADIUS * 0.4, 0.3, RADIUS * 0.4, 0.0);
		Particles.send(world, ParticleTypes.SPLASH, at.x, at.y + 0.2, at.z, 12, 0.6, 0.1, 0.6, 0.1);
		world.playSound((Entity) null, at.x, at.y, at.z, SoundEvents.SPLASH_POTION_BREAK, SoundSource.PLAYERS, 0.8F, 1.0F);
	}

	/** Venomous's Poison II on {@code enemy} for {@code ticks}, unless it counters Venomous. */
	static void poison(ServerPlayer player, LivingEntity enemy, int ticks) {
		if (enemy.isAlive() && !Adaptation.resists(enemy, Perks.VENOMOUS)) {
			enemy.addEffect(new MobEffectInstance(MobEffects.POISON, ticks, POISON_AMPLIFIER), player);
			Particles.send(player.level(), POISON, enemy.getX(), enemy.getY(0.5), enemy.getZ(), 6, 0.3, 0.4, 0.3, 0.0);
		}
	}

	/** Clears every harmful effect on {@code player}. */
	private static void purify(ServerPlayer player) {
		List<Holder<MobEffect>> harmful = new ArrayList<>();
		for (MobEffectInstance effect : player.getActiveEffects()) {
			if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
				harmful.add(effect.getEffect());
			}
		}
		for (Holder<MobEffect> effect : harmful) {
			player.removeEffect(effect);
		}
		if (!harmful.isEmpty()) {
			Particles.send(player.level(), ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY(1.0), player.getZ(), 8, 0.3, 0.4, 0.3, 0.0);
		}
	}
}
