package dev.vadim.bcenchantments.perk;

import dev.vadim.bcenchantments.Particles;
import dev.vadim.bcenchantments.combat.SwingResult;
import dev.vadim.bcenchantments.mob.Adaptation;
import java.util.List;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.phys.Vec3;

/**
 * Charges a little with every hit, more when a swing catches several enemies, and survives misses
 * and pauses for a while. When full, it chains the enemies around the struck target to it, rooting
 * them and the target in place. Synergies:
 * <ul>
 * <li>{@link Synergy#CHAINED_LIGHTNING}: instead of being chained, each of those enemies is struck by
 * lightning at half the weapon's Lightning power and rooted for a moment.</li>
 * <li>{@link Synergy#FROZEN_CHAINS}: everything bound is chilled as by Cold Edge.</li>
 * <li>{@link Synergy#BLACK_HOLE}: every bound enemy pulls the free enemies around it in.</li>
 * <li>{@link Synergy#WHIPLASH}: every bound enemy is yanked to the target before the root holds it.</li>
 * </ul>
 */
public final class Chains implements ChargedPerk {
	static final Chains INSTANCE = new Chains();

	/** Charge is counted in percent: a swing adds {@link #FIRST_HIT} plus {@link #EXTRA_HIT} per further enemy it hits. */
	private static final int FULL = 100;
	private static final int FIRST_HIT = 5;
	private static final int EXTRA_HIT = 2;
	private static final int SEGMENTS = 10;
	/** The charge drops after 8 seconds without a hit. */
	private static final int KEEP_TICKS = 160;

	private static final double RADIUS = 6.0;
	/** Enemies chained for levels 1 to 3, and how long they stay rooted: 0.7 s. */
	private static final int[] COUNT = {2, 3, 4};
	private static final int ROOT_TICKS = 14;
	/** Chained Lightning: share of Lightning's damage each bolt deals, and the root that follows. */
	private static final float CHAINED_BOLT = 0.5F;
	private static final int CHAINED_ROOT_TICKS = 10;
	/** Black Hole: how far around each bound enemy the free ones are pulled from. */
	private static final double BLACK_HOLE_RADIUS = 3.5;
	/** Whiplash: the yank's speed per block of distance, its cap, its lift, and how close is close enough. */
	private static final double WHIPLASH_SPEED = 0.17;
	private static final double WHIPLASH_MAX = 0.9;
	private static final double WHIPLASH_LIFT = 0.3;
	private static final double WHIPLASH_STOP = 1.5;
	private static final DustParticleOptions LINK = new DustParticleOptions(0x8F9AA3, 1.0F);

	private Chains() {
	}

	@Override
	public String name() {
		return "chains";
	}

	@Override
	public ResourceKey<Enchantment> enchantment() {
		return Perks.CHAINS;
	}

	@Override
	public int required(ItemStack weapon, int attacks) {
		return FULL;
	}

	@Override
	public int segments(ItemStack weapon, int attacks) {
		return SEGMENTS;
	}

	@Override
	public int keepTicks(int comboWindowTicks) {
		return KEEP_TICKS;
	}

	@Override
	public boolean resetsOnMiss() {
		return false;
	}

	@Override
	public int advance(int charge, SwingResult swing) {
		return charge + FIRST_HIT + EXTRA_HIT * (swing.hits().size() - 1);
	}

	@Override
	public void fire(SwingResult swing, int level) {
		level = Math.clamp(level, 1, COUNT.length);
		ServerPlayer player = swing.player();
		LivingEntity anchor = swing.primary();
		List<LivingEntity> bound = Targets.enemiesNear(player, anchor.position(), RADIUS, anchor);
		bound = bound.subList(0, Math.min(bound.size(), COUNT[level - 1]));

		int lightning = Perks.level(swing.weapon(), Perks.LIGHTNING);
		boolean chainedLightning = Synergy.CHAINED_LIGHTNING.on(swing.weapon());
		int cold = Synergy.FROZEN_CHAINS.on(swing.weapon()) ? Perks.level(swing.weapon(), Perks.COLD_EDGE) : 0;
		int gravity = Synergy.BLACK_HOLE.on(swing.weapon()) ? Perks.level(swing.weapon(), Perks.GRAVITY) : 0;
		boolean whiplash = Synergy.WHIPLASH.on(swing.weapon());
		List<LivingEntity> held = bound;
		for (LivingEntity target : bound) {
			if (whiplash) {
				yank(player.level(), target, anchor);
			}
			if (gravity > 0) {
				// Each bound enemy pulls in the free ones around it; the bound ones stay put.
				Gravity.pull(player, target.position(), BLACK_HOLE_RADIUS, Gravity.strength(gravity), e -> e == anchor || held.contains(e), true);
			}
			link(player.level(), anchor, target);
			if (chainedLightning) {
				Lightning.strike(player, swing.weapon(), target, Lightning.damage(lightning) * CHAINED_BOLT);
			}
			if (!Adaptation.resists(target, Perks.CHAINS)) {
				Control.root(target, chainedLightning ? CHAINED_ROOT_TICKS : ROOT_TICKS);
			}
			if (cold > 0) {
				ColdEdge.chill(target, cold);
			}
		}
		if (!chainedLightning) {
			if (!Adaptation.resists(anchor, Perks.CHAINS)) {
				Control.root(anchor, ROOT_TICKS);
			}
			if (cold > 0) {
				ColdEdge.chill(anchor, cold);
			}
		}
		player.level().playSound((Entity) null, anchor.getX(), anchor.getY(), anchor.getZ(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1.2F, 0.7F);
		player.level().playSound((Entity) null, anchor.getX(), anchor.getY(), anchor.getZ(), SoundEvents.CHAIN_HIT, SoundSource.PLAYERS, 1.0F, 0.9F);
	}

	/**
	 * Whiplash: throws {@code target} to {@code anchor}, fast enough to land about beside it. A root
	 * only slows walking, so the throw carries it even while rooted. A Wind Charged counter holds it.
	 */
	private static void yank(ServerLevel world, LivingEntity target, LivingEntity anchor) {
		if (Adaptation.resists(target, Perks.WIND_CHARGED)) {
			return;
		}
		Vec3 to = anchor.position().subtract(target.position());
		double distance = to.horizontalDistance();
		if (distance < WHIPLASH_STOP) {
			return;
		}
		Vec3 flat = to.multiply(1.0, 0.0, 1.0).normalize().scale(Math.min((distance - WHIPLASH_STOP) * WHIPLASH_SPEED, WHIPLASH_MAX));
		target.setDeltaMovement(flat.x, WHIPLASH_LIFT, flat.z);
		target.hurtMarked = true;
		Particles.send(world, ParticleTypes.GUST, target.getX(), target.getY(0.5), target.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
	}

	/** A line of iron-grey dust from {@code from} to {@code to}, the chain. */
	private static void link(ServerLevel world, LivingEntity from, LivingEntity to) {
		Vec3 a = from.position().add(0.0, from.getBbHeight() * 0.5, 0.0);
		Vec3 b = to.position().add(0.0, to.getBbHeight() * 0.5, 0.0);
		int steps = Math.max(2, (int) (a.distanceTo(b) / 0.3));
		for (int i = 0; i <= steps; i++) {
			Vec3 p = a.lerp(b, i / (double) steps);
			Particles.send(world, LINK, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
		}
	}
}
