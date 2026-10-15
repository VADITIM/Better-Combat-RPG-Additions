package dev.vadim.bcenchantments.perk;

import dev.vadim.bcenchantments.Particles;
import dev.vadim.bcenchantments.combat.SwingResult;
import dev.vadim.bcenchantments.mob.Adaptation;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.phys.Vec3;

/**
 * Every 12th landed hit releases a burst of souls around the target: it hurts and weakens the enemies
 * close by. The count survives misses and pauses for a while, like Chains. With
 * {@link Synergy#BLACK_LIGHTNING} every bolt of the weapon's lightning releases a burst where it strikes.
 */
public final class Withering implements ChargedPerk {
	static final Withering INSTANCE = new Withering();

	private static final int HITS = 12;
	/** The count drops after 8 seconds without a hit. */
	private static final int KEEP_TICKS = 160;
	private static final float DAMAGE = 5.0F;
	private static final double RADIUS = 3.0;
	/** Weakness for levels 1 to 3: 1, 2 and 3 s. */
	private static final int[] WEAKNESS_TICKS = {20, 40, 60};

	private Withering() {
	}

	@Override
	public String name() {
		return "withering";
	}

	@Override
	public ResourceKey<Enchantment> enchantment() {
		return Perks.WITHERING;
	}

	@Override
	public int required(ItemStack weapon, int attacks) {
		return HITS;
	}

	@Override
	public int keepTicks(int comboWindowTicks) {
		return KEEP_TICKS;
	}

	@Override
	public boolean resetsOnMiss() {
		return false;
	}

	/** Every enemy a swing hits counts. */
	@Override
	public int advance(int charge, SwingResult swing) {
		return charge + swing.hits().size();
	}

	@Override
	public void fire(SwingResult swing, int level) {
		LivingEntity target = swing.primary();
		burst(swing.player(), target.position().add(0.0, target.getBbHeight() * 0.5, 0.0), level);
	}

	/** A burst of souls at {@code at}: hurts and weakens the enemies within reach. */
	static void burst(ServerPlayer player, Vec3 at, int level) {
		level = Math.clamp(level, 1, WEAKNESS_TICKS.length);
		ServerLevel world = player.level();
		for (LivingEntity enemy : Targets.enemiesNear(player, at, RADIUS, null)) {
			enemy.invulnerableTime = 0;
			enemy.hurtServer(world, world.damageSources().indirectMagic(player, player), Adaptation.damage(enemy, Perks.WITHERING, DAMAGE));
			if (!Adaptation.resists(enemy, Perks.WITHERING)) {
				enemy.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, WEAKNESS_TICKS[level - 1], 0), player);
			}
		}
		Particles.send(world, ParticleTypes.SOUL, at.x, at.y, at.z, 24, RADIUS * 0.35, 0.4, RADIUS * 0.35, 0.04);
		Particles.send(world, ParticleTypes.SCULK_SOUL, at.x, at.y, at.z, 8, 0.4, 0.4, 0.4, 0.06);
		world.playSound((Entity) null, at.x, at.y, at.z, SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1.2F, 0.8F);
		world.playSound((Entity) null, at.x, at.y, at.z, SoundEvents.WITHER_SKELETON_AMBIENT, SoundSource.PLAYERS, 0.4F, 1.4F);
	}
}
