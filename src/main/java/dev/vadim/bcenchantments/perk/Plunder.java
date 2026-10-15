package dev.vadim.bcenchantments.perk;

import dev.vadim.bcenchantments.Particles;
import dev.vadim.bcenchantments.combat.CombatHooks;
import dev.vadim.bcenchantments.mob.Adaptation;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * A monster's drops come out larger, and once they have lain for 7 s a share of what is still on the
 * ground turns into emeralds, so a player who waits is paid in the mod's currency. A boss sometimes
 * drops everything twice. With {@link Synergy#GILDED_STORM} a kill by the weapon's lightning drops
 * three times as much instead; with {@link Synergy#WINDFALL} a share of the drops charge up and burst
 * like a wind charge a second later.
 */
final class Plunder {
	/** How much the drops grow for levels 1 to 3. */
	private static final float[] MORE = {0.3F, 0.45F, 0.6F};
	/** Chance each item still on the ground turns into an emerald, and when: 7 s after the kill. */
	private static final float EMERALD_SHARE = 0.1F;
	private static final int CONVERT_TICKS = 140;
	private static final float BOSS_DOUBLE = 0.05F;
	/** Gilded Storm: what a kill by lightning multiplies the drops by. */
	private static final int GILDED_STORM = 3;
	/** Windfall: chance a drop bursts, after how long, how often it crackles meanwhile, and its burst. */
	private static final float WINDFALL_CHANCE = 0.25F;
	private static final int WINDFALL_TICKS = 20;
	private static final int WINDFALL_SPARK_TICKS = 4;
	private static final double WINDFALL_RADIUS = 2.5;
	private static final float WINDFALL_DAMAGE = 2.0F;

	private Plunder() {
	}

	static void onKill(ServerPlayer player, ItemStack weapon, LivingEntity victim) {
		int level = Math.min(Perks.level(weapon, Perks.PLUNDER), MORE.length);
		if (level <= 0 || !(victim instanceof Enemy) || Adaptation.resists(victim, Perks.PLUNDER)) {
			return;
		}
		ServerLevel world = player.level();
		// The kill hooks run after the death loot was dropped: the loot is the items born at the victim this tick.
		List<ItemEntity> drops = world.getEntitiesOfClass(ItemEntity.class, victim.getBoundingBox().inflate(1.0), item -> item.tickCount == 0);
		if (drops.isEmpty()) {
			return;
		}
		Adaptation.used(player, weapon, Perks.PLUNDER);
		RandomSource random = player.getRandom();
		boolean boss = victim instanceof WitherBoss || victim instanceof EnderDragon || victim instanceof ElderGuardian || victim instanceof Warden;
		DamageSource blow = CombatHooks.killingBlow();
		boolean gilded = Synergy.GILDED_STORM.on(weapon) && blow != null && blow.is(DamageTypeTags.IS_LIGHTNING);
		for (ItemEntity drop : drops) {
			ItemStack stack = drop.getItem().copy();
			int count = stack.getCount();
			if (gilded) {
				count *= GILDED_STORM;
			} else {
				count += grow(count * MORE[level - 1], random);
			}
			if (boss && random.nextFloat() < BOSS_DOUBLE) {
				count *= 2;
			}
			stack.setCount(Math.min(count, stack.getMaxStackSize()));
			drop.setItem(stack);
			// What a full stack could not hold comes as a second item.
			if (count > stack.getMaxStackSize()) {
				world.addFreshEntity(new ItemEntity(world, drop.getX(), drop.getY(), drop.getZ(), stack.copyWithCount(count - stack.getMaxStackSize())));
			}
		}
		Particles.send(world, ParticleTypes.HAPPY_VILLAGER, victim.getX(), victim.getY(0.5), victim.getZ(), 6, 0.3, 0.3, 0.3, 0.0);
		CombatHooks.later(CONVERT_TICKS, () -> convert(drops));
		if (Synergy.WINDFALL.on(weapon)) {
			for (ItemEntity drop : drops) {
				if (random.nextFloat() < WINDFALL_CHANCE) {
					windfall(player, drop);
				}
			}
		}
	}

	/**
	 * Windfall: {@code drop} charges for a second, crackling, then bursts like a wind charge from where
	 * it lies. The burst hurts and throws the enemies around it; items stay put. One picked up first
	 * never bursts.
	 */
	private static void windfall(ServerPlayer player, ItemEntity drop) {
		drop.level().playSound((Entity) null, drop.getX(), drop.getY(), drop.getZ(), SoundEvents.BREEZE_CHARGE, SoundSource.PLAYERS, 0.6F, 1.4F);
		for (int tick = 0; tick < WINDFALL_TICKS; tick += WINDFALL_SPARK_TICKS) {
			CombatHooks.later(tick, () -> {
				if (!drop.isRemoved() && drop.level() instanceof ServerLevel world) {
					Particles.send(world, ParticleTypes.SMALL_GUST, drop.getX(), drop.getY() + 0.3, drop.getZ(), 1, 0.15, 0.1, 0.15, 0.0);
					Particles.send(world, ParticleTypes.ELECTRIC_SPARK, drop.getX(), drop.getY() + 0.3, drop.getZ(), 2, 0.1, 0.1, 0.1, 0.05);
				}
			});
		}
		CombatHooks.later(WINDFALL_TICKS, () -> {
			if (!drop.isRemoved() && !player.isRemoved() && drop.level() == player.level()) {
				WindBurst.at(player, drop.position().add(0.0, 0.3, 0.0), WINDFALL_RADIUS, WINDFALL_DAMAGE, null);
			}
		});
	}

	/** {@code amount} as a whole count: the fraction left over is the chance of one more. */
	private static int grow(float amount, RandomSource random) {
		int whole = (int) amount;
		return whole + (random.nextFloat() < amount - whole ? 1 : 0);
	}

	/** Turns a share of the drops still lying on the ground into emeralds. */
	private static void convert(List<ItemEntity> drops) {
		for (ItemEntity drop : drops) {
			if (drop.isRemoved() || !(drop.level() instanceof ServerLevel world) || drop.getItem().is(Items.EMERALD)) {
				continue;
			}
			ItemStack stack = drop.getItem();
			int emeralds = 0;
			for (int i = 0; i < stack.getCount(); i++) {
				if (world.getRandom().nextFloat() < EMERALD_SHARE) {
					emeralds++;
				}
			}
			if (emeralds == 0) {
				continue;
			}
			ItemStack rest = stack.copyWithCount(stack.getCount() - emeralds);
			if (rest.isEmpty()) {
				drop.setItem(new ItemStack(Items.EMERALD, emeralds));
			} else {
				drop.setItem(rest);
				world.addFreshEntity(new ItemEntity(world, drop.getX(), drop.getY(), drop.getZ(), new ItemStack(Items.EMERALD, emeralds)));
			}
			Particles.send(world, ParticleTypes.HAPPY_VILLAGER, drop.getX(), drop.getY() + 0.3, drop.getZ(), 8, 0.2, 0.2, 0.2, 0.0);
			world.playSound((Entity) null, drop.getX(), drop.getY(), drop.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.4F);
		}
	}
}
