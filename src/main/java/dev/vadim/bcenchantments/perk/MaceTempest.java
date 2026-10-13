package dev.vadim.bcenchantments.perk;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * {@link Synergy#TEMPEST} on a mace: a smash attack also hurts the enemies around the impact, on the
 * ground at the struck target's feet, for part of the smash's damage.
 */
final class MaceTempest {
	private static final double RADIUS = 2.5;
	private static final float SHARE = 0.3F;

	/** Set while the splash deals its damage, so the splash does not splash again. */
	private static boolean splashing;

	private MaceTempest() {
	}

	/** After any entity took damage: splashes it if the damage was a Tempest mace's smash. */
	static void afterDamage(LivingEntity target, DamageSource source, float baseDamage, float damage, boolean blocked) {
		if (splashing || blocked || baseDamage <= 0.0F || !(source.getDirectEntity() instanceof ServerPlayer player) || source.getEntity() != player) {
			return;
		}
		ItemStack weapon = source.getWeaponItem();
		if (weapon == null || !(weapon.getItem() instanceof MaceItem) || !MaceItem.canSmashAttack(player) || !Synergy.TEMPEST.on(weapon)) {
			return;
		}
		ServerLevel world = player.level();
		Vec3 impact = target.position();
		float splash = baseDamage * SHARE;
		splashing = true;
		try {
			for (LivingEntity enemy : Targets.enemiesNear(player, impact, RADIUS, target)) {
				enemy.invulnerableTime = 0;
				enemy.hurtServer(world, world.damageSources().playerAttack(player), splash);
			}
		} finally {
			splashing = false;
		}
		BlockState floor = world.getBlockState(target.blockPosition().below());
		if (!floor.isAir()) {
			world.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, floor), impact.x, impact.y + 0.1, impact.z, 12, RADIUS * 0.4, 0.05, RADIUS * 0.4, 0.15);
		}
		world.sendParticles(ParticleTypes.GUST_EMITTER_SMALL, impact.x, impact.y + 0.2, impact.z, 1, 0.0, 0.0, 0.0, 0.0);
		world.playSound((Entity) null, impact.x, impact.y, impact.z, SoundEvents.WIND_CHARGE_BURST, SoundSource.PLAYERS, 0.7F, 0.8F);
	}
}
