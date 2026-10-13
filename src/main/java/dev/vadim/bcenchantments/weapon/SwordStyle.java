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
 * The sword keeps Better Combat's combo, with two special swings on top:
 * <ul>
 * <li>Air crit: any swing in the air that rolled a critical hit becomes a two-handed strike, the
 * sword held over the centre of the head and brought straight down.</li>
 * <li>Uppercut: on the ground, with the Uppercut power, the third swing becomes a rising cut that
 * launches what it hits.</li>
 * </ul>
 */
public final class SwordStyle implements WeaponStyle {
	public static final SwordStyle INSTANCE = new SwordStyle();

	public static final String UPPERCUT = RpgAdditions.MOD_ID + ":uppercut";
	public static final String AIR_CRIT = RpgAdditions.MOD_ID + ":air_crit";

	private static final int SPECIAL_SWING = 3;
	// Weapon trails, after Better Combat's own one-handed uppercut and two-handed slam; the air crit's is centred.
	private static final List<ParticlePlacement> UPPERCUT_TRAIL = List.of(new ParticlePlacement("slash90", 0.0F, -0.1F, 0.0F, 0.0F, 0.0F, 75.0F));
	private static final List<ParticlePlacement> AIR_CRIT_TRAIL = List.of(new ParticlePlacement("slash180", 0.0F, -0.1F, 0.0F, 45.0F, 0.0F, -90.0F));

	private SwordStyle() {
	}

	@Override
	public boolean matches(ItemStack stack) {
		return stack.is(ItemTags.SWORDS);
	}

	@Override
	public AttackHand replace(Player player, AttackHand hand) {
		if (hand.isOffHand()) {
			return hand;
		}
		if (isAirborne(player)) {
			return CriticalHits.next(player, false) ? with(hand, AIR_CRIT, 60.0, AIR_CRIT_TRAIL) : hand;
		}
		if (hand.combo().current() == SPECIAL_SWING && Perks.level(hand.itemStack(), Perks.UPPERCUT) > 0) {
			return with(hand, UPPERCUT, 70.0, UPPERCUT_TRAIL);
		}
		return hand;
	}

	@Override
	public boolean launches(Player player, String animation) {
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
