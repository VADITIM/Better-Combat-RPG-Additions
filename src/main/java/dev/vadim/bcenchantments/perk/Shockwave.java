package dev.vadim.bcenchantments.perk;

import dev.vadim.bcenchantments.combat.SwingResult;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A crescent slash that flies out where the player looks, like a sword beam, after three swings in
 * a row that each hit three or more enemies. It cuts through everything in its path once, kicks up
 * the ground it passes over like the Dungeons shockwave, and breaks on walls. With
 * {@link Synergy#TEMPEST} it throws what it hits much harder, and an enemy slammed into a wall takes
 * knockback damage. With {@link Synergy#SINGULARITY} it drags the enemies beside its path into it.
 */
public final class Shockwave implements ChargedPerk {
	static final Shockwave INSTANCE = new Shockwave();

	private static final int MIN_TARGETS = 3;
	private static final int SWINGS = 3;
	/** Extra time on top of Better Combat's combo window that the swing streak survives a pause: 0.6 s. */
	private static final int STREAK_GRACE_TICKS = 12;

	/** Damage in health points (2 per heart) and reach in blocks for levels 1 to 3. */
	private static final float[] DAMAGE = {4.0F, 6.0F, 8.0F};
	private static final double[] RANGE = {8.0, 10.0, 12.0};
	private static final double SPEED = 0.9;
	private static final double HALF_WIDTH = 1.6;
	private static final double HEIGHT = 0.9;
	private static final double PUSH = 0.6;
	/** Tempest: how hard the wave throws, and the wall damage for levels 1 to 3. */
	private static final double TEMPEST_PUSH = 1.4;
	private static final double TEMPEST_LIFT = 0.3;
	private static final float[] TEMPEST_DAMAGE = {4.0F, 6.0F, 8.0F};
	private static final int TEMPEST_TRACK_TICKS = 25;
	/** Singularity: reach of the pull around a point this far ahead of the blade, and its share of Gravity's pull. */
	private static final double SINGULARITY_RADIUS = 4.0;
	private static final double SINGULARITY_LEAD = 1.5;
	private static final double SINGULARITY_PULL = 0.8;

	private static final class Wave {
		final ServerPlayer owner;
		final ServerLevel level;
		final Vec3 dir;
		final Vec3 side;
		final double range;
		final float damage;
		/** Tempest's wall damage, 0 without it. */
		final float slam;
		/** Singularity's pull speed, 0 without it. */
		final double gravity;
		final Set<Entity> hit = new HashSet<>();
		Vec3 pos;
		double travelled;

		Wave(ServerPlayer owner, ServerLevel level, Vec3 pos, Vec3 dir, int enchantLevel, boolean tempest, int gravity) {
			this.owner = owner;
			this.level = level;
			this.pos = pos;
			this.dir = dir;
			this.side = new Vec3(-dir.z, 0.0, dir.x);
			this.range = RANGE[enchantLevel - 1];
			this.damage = DAMAGE[enchantLevel - 1];
			this.slam = tempest ? TEMPEST_DAMAGE[enchantLevel - 1] : 0.0F;
			this.gravity = gravity > 0 ? Gravity.strength(gravity) * SINGULARITY_PULL : 0.0;
		}
	}

	private static final List<Wave> WAVES = new ArrayList<>();

	private Shockwave() {
	}

	@Override
	public String name() {
		return "shockwave";
	}

	@Override
	public ResourceKey<Enchantment> enchantment() {
		return Perks.SHOCKWAVE;
	}

	@Override
	public int required(ItemStack weapon, int attacks) {
		return SWINGS;
	}

	@Override
	public int keepTicks(int comboWindowTicks) {
		return comboWindowTicks + STREAK_GRACE_TICKS;
	}

	/** Only swings that each hit a crowd count; one that does not starts the streak over. */
	@Override
	public int advance(int charge, SwingResult swing) {
		return swing.hits().size() >= MIN_TARGETS ? charge + 1 : 0;
	}

	@Override
	public void fire(SwingResult swing, int level) {
		int gravity = Synergy.SINGULARITY.on(swing.weapon()) ? Perks.level(swing.weapon(), Perks.GRAVITY) : 0;
		launch(swing.player(), level, Synergy.TEMPEST.on(swing.weapon()), gravity);
	}

	private static void launch(ServerPlayer player, int level, boolean tempest, int gravity) {
		level = Math.min(level, DAMAGE.length);
		ServerLevel world = player.level();
		Vec3 look = player.getLookAngle();
		Vec3 dir = new Vec3(look.x, 0.0, look.z);
		if (dir.lengthSqr() < 1.0E-4) {
			// Looking straight up or down: fall back to the way the body faces.
			double yaw = Math.toRadians(player.getYRot());
			dir = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
		}
		dir = dir.normalize();
		Vec3 start = player.position().add(0.0, HEIGHT, 0.0).add(dir.scale(0.6));
		WAVES.add(new Wave(player, world, start, dir, level, tempest, gravity));

		world.playSound((Entity) null, start.x, start.y, start.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.6F);
		world.playSound((Entity) null, start.x, start.y, start.z, SoundEvents.TRIDENT_RIPTIDE_1, SoundSource.PLAYERS, 0.7F, 1.4F);
		world.playSound((Entity) null, start.x, start.y, start.z, SoundEvents.BREEZE_SHOOT, SoundSource.PLAYERS, 0.8F, 1.2F);
		// The ring of dust a Dungeons shockwave throws up around the player.
		for (int i = 0; i < 6; i++) {
			double angle = Math.PI * 2.0 * i / 6.0;
			double x = player.getX() + Math.cos(angle) * 1.2;
			double z = player.getZ() + Math.sin(angle) * 1.2;
			world.sendParticles(ParticleTypes.POOF, x, player.getY() + 0.1, z, 0, Math.cos(angle), 0.05, Math.sin(angle), 0.15);
		}
		ground(world, player.position().add(0.0, HEIGHT, 0.0), 6, 1.2);
	}

	static void tick() {
		Iterator<Wave> it = WAVES.iterator();
		while (it.hasNext()) {
			Wave wave = it.next();
			if (wave.owner.isRemoved() || wave.owner.level() != wave.level) {
				it.remove();
				continue;
			}
			wave.pos = wave.pos.add(wave.dir.scale(SPEED));
			wave.travelled += SPEED;

			BlockPos at = BlockPos.containing(wave.pos);
			if (!wave.level.getBlockState(at).getCollisionShape(wave.level, at).isEmpty()) {
				shatter(wave);
				it.remove();
				continue;
			}

			AABB box = new AABB(wave.pos, wave.pos).inflate(HALF_WIDTH, 1.1, HALF_WIDTH);
			for (LivingEntity target : wave.level.getEntitiesOfClass(LivingEntity.class, box, e -> canHit(wave, e))) {
				wave.hit.add(target);
				target.invulnerableTime = 0;
				target.hurtServer(wave.level, wave.level.damageSources().playerAttack(wave.owner), wave.damage);
				if (wave.slam > 0.0F) {
					target.setDeltaMovement(wave.dir.x * TEMPEST_PUSH, TEMPEST_LIFT, wave.dir.z * TEMPEST_PUSH);
					Impact.track(target, wave.owner, wave.slam, TEMPEST_TRACK_TICKS);
				} else {
					target.push(wave.dir.x * PUSH, 0.25, wave.dir.z * PUSH);
				}
				target.hurtMarked = true;
				wave.level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(0.5), target.getZ(), 4, 0.3, 0.3, 0.3, 0.3);
			}

			if (wave.gravity > 0.0) {
				// Singularity: drags the enemies beside the path in, ahead of the blade.
				Vec3 ahead = wave.pos.add(wave.dir.scale(SINGULARITY_LEAD));
				Gravity.pull(wave.owner, ahead, SINGULARITY_RADIUS, wave.gravity, wave.hit::contains, false);
			}
			draw(wave);
			if (wave.travelled >= wave.range) {
				shatter(wave);
				it.remove();
			}
		}
	}

	private static boolean canHit(Wave wave, LivingEntity target) {
		return target != wave.owner
			&& target.isAlive()
			&& !target.isSpectator()
			&& !wave.hit.contains(target)
			&& !target.isAlliedTo(wave.owner)
			&& !(target instanceof TamableAnimal pet && pet.isOwnedBy(wave.owner));
	}

	private static void draw(Wave wave) {
		ServerLevel world = wave.level;
		// The blade: a crescent bowed back at the tips, bright like a sword beam.
		for (int k = -2; k <= 2; k++) {
			double t = k / 2.0;
			Vec3 p = wave.pos.add(wave.side.scale(t * HALF_WIDTH)).subtract(wave.dir.scale(t * t * 0.8));
			double lift = (1.0 - t * t) * 0.2;
			world.sendParticles(ParticleTypes.END_ROD, p.x, p.y + lift, p.z, 1, 0.02, 0.02, 0.02, 0.0);
			if ((k & 1) != 0) {
				world.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y + lift, p.z, 1, 0.05, 0.08, 0.05, 0.05);
			}
		}
		world.sendParticles(ParticleTypes.SWEEP_ATTACK, wave.pos.x, wave.pos.y, wave.pos.z, 0, 0.0, 0.0, 0.0, 0.0);
		// A trail of wind behind the blade, two steps in five.
		if (Math.round(wave.travelled / SPEED) % 5 < 2) {
			Vec3 tail = wave.pos.subtract(wave.dir.scale(0.8));
			world.sendParticles(ParticleTypes.SMALL_GUST, tail.x, tail.y, tail.z, 1, 0.4, 0.1, 0.4, 0.0);
		}
		ground(world, wave.pos, 3, HALF_WIDTH * 0.5);
	}

	/** Kicks up the block under {@code pos}, the cracking-earth look of the Dungeons shockwave. */
	private static void ground(ServerLevel world, Vec3 pos, int count, double spread) {
		BlockPos floor = BlockPos.containing(pos.x, pos.y - HEIGHT - 0.5, pos.z);
		BlockState state = world.getBlockState(floor);
		if (state.isAir()) {
			return;
		}
		world.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.x, floor.getY() + 1.05, pos.z, count, spread, 0.05, spread, 0.15);
		world.sendParticles(new BlockParticleOption(ParticleTypes.DUST_PILLAR, state), pos.x, floor.getY() + 1.0, pos.z, Math.max(1, count / 3), spread, 0.0, spread, 0.0);
	}

	private static void shatter(Wave wave) {
		Vec3 p = wave.pos;
		wave.level.sendParticles(ParticleTypes.GUST_EMITTER_SMALL, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
		wave.level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 5, 0.3, 0.3, 0.3, 0.12);
		wave.level.playSound((Entity) null, p.x, p.y, p.z, SoundEvents.WIND_CHARGE_BURST, SoundSource.PLAYERS, 0.6F, 1.3F);
	}
}
