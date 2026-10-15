package dev.vadim.bcenchantments.perk;

import dev.vadim.bcenchantments.Particles;
import dev.vadim.bcenchantments.combat.SwingResult;
import dev.vadim.bcenchantments.mob.Adaptation;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Each hit has a chance to curse its target: one random harmful effect for a moment. A cursed enemy
 * stays cursed while the effect lasts, which other powers' combinations read. With
 * {@link Synergy#HEXBOLT} every curse that lands also calls a quarter-power lightning bolt on its target.
 */
final class Curse {
	/** Chance a hit curses, and how long the curse lasts (1.5, 2 and 2.5 s), for levels 1 to 3. */
	private static final float[] CHANCE = {0.05F, 0.10F, 0.15F};
	private static final int[] TICKS = {30, 40, 50};
	/** What a curse can be, each with the amplifier it comes at. */
	private static final List<Hex> HEXES = List.of(
		new Hex(MobEffects.SLOWNESS, 1),
		new Hex(MobEffects.WEAKNESS, 0),
		new Hex(MobEffects.WITHER, 0),
		new Hex(MobEffects.POISON, 0),
		new Hex(MobEffects.BLINDNESS, 0));
	private static final float HEXBOLT = 0.25F;

	private record Hex(Holder<MobEffect> effect, int amplifier) {
	}

	/** Cursed enemies, until the tick their curse ends. */
	private static final Map<LivingEntity, Long> CURSED = new WeakHashMap<>();
	private static long now;

	private Curse() {
	}

	static void onSwing(SwingResult swing) {
		for (LivingEntity hit : swing.hits()) {
			roll(swing.player(), swing.weapon(), hit);
		}
	}

	static void onDirectHit(ServerPlayer player, ItemStack weapon, LivingEntity target) {
		roll(player, weapon, target);
	}

	private static void roll(ServerPlayer player, ItemStack weapon, LivingEntity target) {
		int level = Math.min(Perks.level(weapon, Perks.CURSE), CHANCE.length);
		if (level > 0 && target.isAlive() && player.getRandom().nextFloat() < CHANCE[level - 1]) {
			apply(player, weapon, target, level);
		}
	}

	/** Curses {@code target} with a random harmful effect for {@code level}'s time; false if it counters Curse. */
	static boolean apply(ServerPlayer player, ItemStack weapon, LivingEntity target, int level) {
		if (Adaptation.resists(target, Perks.CURSE)) {
			return false;
		}
		level = Math.clamp(level, 1, TICKS.length);
		Hex hex = HEXES.get(player.getRandom().nextInt(HEXES.size()));
		int ticks = TICKS[level - 1];
		target.addEffect(new MobEffectInstance(hex.effect(), ticks, hex.amplifier()), player);
		CURSED.put(target, now + ticks);
		Adaptation.used(player, weapon, Perks.CURSE);
		ServerLevel world = player.level();
		Particles.send(world, ParticleTypes.WITCH, target.getX(), target.getY(0.8), target.getZ(), 12, 0.3, 0.3, 0.3, 0.05);
		world.playSound((Entity) null, target.getX(), target.getY(), target.getZ(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.5F, 1.6F);
		if (Synergy.HEXBOLT.on(weapon)) {
			Lightning.strike(player, weapon, target, Lightning.damage(Perks.level(weapon, Perks.LIGHTNING)) * HEXBOLT);
		}
		return true;
	}

	/** Whether {@code target} is cursed right now. */
	static boolean cursed(LivingEntity target) {
		Long until = CURSED.get(target);
		return until != null && now <= until && target.isAlive();
	}

	static void tick() {
		now++;
		CURSED.values().removeIf(until -> until < now);
	}
}
