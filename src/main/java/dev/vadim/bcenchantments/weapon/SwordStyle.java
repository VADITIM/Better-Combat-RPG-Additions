package dev.vadim.bcenchantments.weapon;

import dev.vadim.bcenchantments.RpgAdditions;
import dev.vadim.bcenchantments.combat.CriticalHits;
import dev.vadim.bcenchantments.perk.Perks;
import java.util.List;
import net.bettercombat.api.AttackHand;
import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.api.fx.ParticlePlacement;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The sword's third swing changes with the situation:
 * <ul>
 * <li>Slam: swung while airborne, it comes down in an overhead slam, two-handed when the swing rolled
 * a critical hit, one-handed otherwise.</li>
 * <li>Uppercut: on the ground, with the Uppercut perk, it becomes a rising cut that launches what it hits.</li>
 * </ul>
 */
public final class SwordStyle implements WeaponStyle {
	public static final SwordStyle INSTANCE = new SwordStyle();

	public static final String UPPERCUT = RpgAdditions.MOD_ID + ":uppercut";
	public static final String SLAM = RpgAdditions.MOD_ID + ":slam";
	public static final String SLAM_CRIT = RpgAdditions.MOD_ID + ":slam_crit";

	private static final int SPECIAL_SWING = 3;
	// Weapon trails, taken from Better Combat's own one-handed uppercut, one-handed slam and two-handed slam.
	private static final List<ParticlePlacement> UPPERCUT_TRAIL = List.of(new ParticlePlacement("slash90", 0.0F, -0.1F, 0.0F, 0.0F, 0.0F, 75.0F));
	private static final List<ParticlePlacement> SLAM_TRAIL = List.of(new ParticlePlacement("slash90", 0.05F, -0.1F, 0.0F, 45.0F, 0.0F, -85.0F));
	private static final List<ParticlePlacement> SLAM_CRIT_TRAIL = List.of(new ParticlePlacement("slash180", 0.1F, -0.1F, 0.0F, 45.0F, 0.0F, -86.0F));

	private SwordStyle() {
	}

	@Override
	public boolean matches(ItemStack stack) {
		return stack.is(ItemTags.SWORDS);
	}

	@Override
	public AttackHand replace(Player player, AttackHand hand) {
		if (hand.isOffHand() || hand.combo().current() != SPECIAL_SWING) {
			return hand;
		}
		if (isAirborne(player)) {
			return CriticalHits.next(player, false) ? with(hand, SLAM_CRIT, 60.0, SLAM_CRIT_TRAIL) : with(hand, SLAM, 60.0, SLAM_TRAIL);
		}
		if (Perks.level(hand.itemStack(), Perks.UPPERCUT) > 0) {
			return with(hand, UPPERCUT, 70.0, UPPERCUT_TRAIL);
		}
		return hand;
	}

	@Override
	public boolean launches(String animation) {
		return UPPERCUT.equals(animation);
	}

	private static boolean isAirborne(Player player) {
		return !player.onGround()
			&& !player.isInWater()
			&& !player.onClimbable()
			&& !player.isPassenger()
			&& !player.isFallFlying()
			&& !player.getAbilities().flying;
	}

	private static AttackHand with(AttackHand hand, String animation, double angle, List<ParticlePlacement> trail) {
		WeaponAttributes.Attack base = hand.attack();
		WeaponAttributes.Attack attack = new WeaponAttributes.Attack(
			base.conditions(), WeaponAttributes.HitBoxShape.VERTICAL_PLANE,
			base.damageMultiplier(), base.movementSpeedMultiplier(), base.rangeMultiplier(),
			angle, base.upswing(), animation,
			base.swingSound(), base.impactSound(), trail
		);
		return new AttackHand(attack, hand.combo(), hand.isOffHand(), hand.attributes(), hand.itemStack());
	}
}
