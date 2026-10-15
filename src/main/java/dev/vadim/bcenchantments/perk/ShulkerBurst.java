package dev.vadim.bcenchantments.perk;

import dev.vadim.bcenchantments.TrimEnchantments;
import dev.vadim.bcenchantments.combat.CombatHooks;
import dev.vadim.bcenchantments.combat.SwingResult;
import dev.vadim.bcenchantments.mob.Adaptation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Every 8th landed hit releases shulker bullets from the player, each homing in on a random enemy
 * nearby. A bullet that hits levitates its target for 2 s, not a shulker's 10. The count survives
 * misses and pauses for a while, like Chains. With {@link Synergy#SHULKER_STORM} a bullet that hits
 * calls a quarter-power lightning bolt on its target; with {@link Synergy#SHULKER_GUST} it throws its
 * target back like a small gust.
 */
public final class ShulkerBurst implements ChargedPerk {
	static final ShulkerBurst INSTANCE = new ShulkerBurst();

	private static final int HITS = 8;
	private static final int KEEP_TICKS = 160;
	/** Bullets for levels 1 to 3, and how far away their targets may be. */
	private static final int[] BULLETS = {1, 2, 3};
	private static final double RANGE = 12.0;
	private static final int LEVITATION_TICKS = 40;
	private static final float SHULKER_STORM = 0.25F;
	/** Shulker Gust: the throw's share of a death burst's push. */
	private static final double SHULKER_GUST = 0.8;
	private static final String TAG = TrimEnchantments.MOD_ID + ".shulker_burst";

	/** The bullets in flight, with the player and weapon that fired them. */
	private record Shot(ServerPlayer player, ItemStack weapon) {
	}

	private static final Map<ShulkerBullet, Shot> SHOTS = new WeakHashMap<>();

	private ShulkerBurst() {
	}

	@Override
	public String name() {
		return "shulker_burst";
	}

	@Override
	public ResourceKey<Enchantment> enchantment() {
		return Perks.SHULKER_BURST;
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
		release(swing.player(), swing.weapon(), BULLETS[Math.clamp(level, 1, BULLETS.length) - 1]);
	}

	/** Fires {@code count} bullets from {@code player}, each at a different random enemy while there are enough. */
	static void release(ServerPlayer player, ItemStack weapon, int count) {
		ServerLevel world = player.level();
		List<LivingEntity> targets = new ArrayList<>(Targets.enemiesNear(player, player.position(), RANGE, null));
		if (targets.isEmpty()) {
			return;
		}
		for (int i = 0; i < count; i++) {
			if (targets.isEmpty()) {
				targets.addAll(Targets.enemiesNear(player, player.position(), RANGE, null));
			}
			LivingEntity target = targets.remove(player.getRandom().nextInt(targets.size()));
			ShulkerBullet bullet = new ShulkerBullet(world, player, target, Direction.Axis.Y);
			bullet.addTag(TAG);
			SHOTS.put(bullet, new Shot(player, weapon.copy()));
			world.addFreshEntity(bullet);
		}
		world.playSound((Entity) null, player.getX(), player.getY(), player.getZ(), SoundEvents.SHULKER_SHOOT, SoundSource.PLAYERS, 1.0F, 1.2F);
	}

	/** A bullet of this power that hit: its levitation is cut to 2 s, and Shulker Storm strikes. */
	static void afterDamage(LivingEntity target, DamageSource source, float baseDamage, float damage, boolean blocked) {
		if (!(source.getDirectEntity() instanceof ShulkerBullet bullet) || !bullet.entityTags().contains(TAG)) {
			return;
		}
		Shot shot = SHOTS.remove(bullet);
		// The bullet adds its own long levitation after the damage, so this waits a tick to replace it.
		CombatHooks.later(1, () -> {
			if (!target.isAlive()) {
				return;
			}
			target.removeEffect(MobEffects.LEVITATION);
			if (!Adaptation.resists(target, Perks.SHULKER_BURST)) {
				target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, LEVITATION_TICKS, 0));
			}
		});
		if (shot == null || shot.player().isRemoved()) {
			return;
		}
		if (Synergy.SHULKER_STORM.on(shot.weapon())) {
			Lightning.strike(shot.player(), shot.weapon(), target,
				Lightning.damage(Perks.level(shot.weapon(), Perks.LIGHTNING)) * SHULKER_STORM);
		}
		if (Synergy.SHULKER_GUST.on(shot.weapon()) && shot.player().level() == target.level()) {
			WindBurst.gust(shot.player(), target, SHULKER_GUST);
		}
	}

	static void tick() {
		SHOTS.keySet().removeIf(Entity::isRemoved);
	}
}
