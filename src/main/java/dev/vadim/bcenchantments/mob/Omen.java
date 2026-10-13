package dev.vadim.bcenchantments.mob;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Opt-in difficulty: drinking an ominous bottle (Bad Omen I to V) makes monsters that spawn near the
 * player sturdier ({@link MobScaling}), and pays for it with loot: a monster the player kills may drop
 * an emerald, and at Bad Omen V, rarely an echo shard.
 */
public final class Omen {
	private Omen() {
	}

	public static void init() {
		ServerLivingEntityEvents.AFTER_DEATH.register(Omen::onDeath);
	}

	/** The player's Bad Omen level, 1 to 5, or 0 without it. */
	public static int level(Player player) {
		MobEffectInstance omen = player.getEffect(MobEffects.BAD_OMEN);
		return omen == null ? 0 : Math.min(omen.getAmplifier() + 1, MobBalance.OMEN_MAX);
	}

	private static void onDeath(LivingEntity entity, DamageSource source) {
		if (!(entity instanceof Enemy) || !(source.getEntity() instanceof ServerPlayer player)
			|| !(entity.level() instanceof ServerLevel world)) {
			return;
		}
		int level = level(player);
		if (level == 0) {
			return;
		}
		RandomSource random = entity.getRandom();
		if (random.nextFloat() < MobBalance.OMEN_EMERALD_BASE + MobBalance.OMEN_EMERALD_PER_LEVEL * level) {
			entity.spawnAtLocation(world, new ItemStack(Items.EMERALD));
		}
		if (level >= MobBalance.OMEN_MAX && random.nextFloat() < MobBalance.OMEN_ECHO_SHARD) {
			entity.spawnAtLocation(world, new ItemStack(Items.ECHO_SHARD));
		}
	}
}
