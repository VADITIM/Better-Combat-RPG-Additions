package dev.vadim.bcenchantments.perk;

import dev.vadim.bcenchantments.Particles;
import dev.vadim.bcenchantments.combat.CombatHooks;
import dev.vadim.bcenchantments.combat.ComboTracker;
import dev.vadim.bcenchantments.combat.SwingResult;
import dev.vadim.bcenchantments.mob.Adaptation;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Every fourth landed swing echoes: a moment later everything it hit is struck again. The echo is a
 * plain extra attack. It runs no swing perks of its own, such as Cold Edge, but it counts toward the
 * charge of the hand's other perks like any hit. With {@link Synergy#RESONANCE} the echo also sparks
 * a quarter-power lightning bolt on the swing's target; with {@link Synergy#FRENZY} it echoes every
 * third swing; with {@link Synergy#GALE_ECHO} each strike pushes its target back like a small gust.
 */
public final class Echo implements ChargedPerk {
	static final Echo INSTANCE = new Echo();

	private static final int SWINGS = 4;
	/** Frenzy: on a fully refined weapon it echoes every third swing. */
	private static final int FRENZY_SWINGS = 3;
	/** Extra time on top of Better Combat's combo window that the count survives a pause. */
	private static final int GRACE_TICKS = 12;
	private static final int DELAY_TICKS = 6;
	/** Share of the swing's damage the echo deals, for levels 1 to 3. */
	private static final float[] DAMAGE = {0.5F, 0.65F, 0.8F};
	private static final float RESONANCE_BOLT = 0.25F;
	/** Gale Echo: the gust's share of a death burst's push. */
	private static final double GALE_PUSH = 0.6;

	private Echo() {
	}

	@Override
	public String name() {
		return "echo";
	}

	@Override
	public ResourceKey<Enchantment> enchantment() {
		return Perks.ECHO;
	}

	@Override
	public int required(ItemStack weapon, int attacks) {
		return Synergy.FRENZY.on(weapon) ? FRENZY_SWINGS : SWINGS;
	}

	@Override
	public int keepTicks(int comboWindowTicks) {
		return comboWindowTicks + GRACE_TICKS;
	}

	@Override
	public int advance(int charge, SwingResult swing) {
		return charge + 1;
	}

	@Override
	public void fire(SwingResult swing, int level) {
		ServerPlayer player = swing.player();
		float damage = (float) (player.getAttributeValue(Attributes.ATTACK_DAMAGE) * swing.hand().attack().damageMultiplier())
			* DAMAGE[Math.clamp(level, 1, DAMAGE.length) - 1];
		float resonance = Synergy.RESONANCE.on(swing.weapon())
			? Lightning.damage(Perks.level(swing.weapon(), Perks.LIGHTNING)) * RESONANCE_BOLT
			: 0.0F;
		CombatHooks.later(DELAY_TICKS, () -> strike(swing, damage, resonance));
	}

	private void strike(SwingResult swing, float damage, float resonance) {
		ServerPlayer player = swing.player();
		if (player.isRemoved()) {
			return;
		}
		ServerLevel world = player.level();
		List<LivingEntity> struck = new ArrayList<>();
		boolean gale = Synergy.GALE_ECHO.on(swing.weapon());
		for (LivingEntity target : swing.hits()) {
			if (!target.isAlive() || target.level() != world) {
				continue;
			}
			target.invulnerableTime = 0;
			if (target.hurtServer(world, world.damageSources().playerAttack(player), Adaptation.damage(target, Perks.ECHO, damage))) {
				struck.add(target);
				if (gale) {
					WindBurst.gust(player, target, GALE_PUSH);
				}
			}
			Particles.send(world, ParticleTypes.SCULK_CHARGE_POP, target.getX(), target.getY(0.5), target.getZ(), 8, 0.3, 0.4, 0.3, 0.02);
			Particles.send(world, ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY(0.5), target.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
		}
		if (resonance > 0.0F) {
			Lightning.strike(player, swing.weapon(), swing.primary(), resonance);
		}
		LivingEntity at = swing.primary();
		world.playSound((Entity) null, at.getX(), at.getY(), at.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.4F);
		world.playSound((Entity) null, at.getX(), at.getY(), at.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 0.7F, 1.3F);
		ComboTracker.countExtraAttack(player, swing.hand(), struck, this);
	}
}
