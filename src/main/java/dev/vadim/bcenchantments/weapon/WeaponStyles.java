package dev.vadim.bcenchantments.weapon;

import java.util.ArrayList;
import java.util.List;
import net.bettercombat.api.AttackHand;
import net.bettercombat.logic.PlayerAttackHelper;
import net.bettercombat.logic.PlayerAttackProperties;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** The registered {@link WeaponStyle}s, and the lookups the mixins go through. */
public final class WeaponStyles {
	private static final List<WeaponStyle> STYLES = new ArrayList<>();

	private WeaponStyles() {
	}

	public static void init() {
		// Spears before swords: a modded spear could carry the sword tag too.
		register(SpearStyle.INSTANCE);
		register(SwordStyle.INSTANCE);
		register(MaceStyle.INSTANCE);
	}

	public static void register(WeaponStyle style) {
		STYLES.add(style);
	}

	public static @Nullable WeaponStyle of(ItemStack stack) {
		if (stack.isEmpty()) {
			return null;
		}
		for (WeaponStyle style : STYLES) {
			if (style.matches(stack)) {
				return style;
			}
		}
		return null;
	}

	public static AttackHand replace(Player player, AttackHand hand) {
		WeaponStyle style = of(hand.itemStack());
		return style == null ? hand : style.replace(player, hand);
	}

	/**
	 * How much longer or shorter the coming swing's cooldown is. Both sides read it at the moment that
	 * counts: the client while it waits to start and land the swing, and the server while it runs the
	 * swing, when Better Combat has set the player's combo count to that swing.
	 */
	public static float cooldownFactor(Player player) {
		if (!shapesCooldown(player.getMainHandItem()) && !shapesCooldown(player.getOffhandItem())) {
			return 1.0F;
		}
		int combo = ((PlayerAttackProperties) player).getComboCount();
		if (combo < 0) {
			return 1.0F;
		}
		AttackHand hand = PlayerAttackHelper.getCurrentAttack(player, combo);
		WeaponStyle style = hand == null ? null : of(hand.itemStack());
		return style == null ? 1.0F : style.cooldownFactor(hand.attack().animation());
	}

	public static double knockbackFactor(AttackHand swing) {
		WeaponStyle style = of(swing.itemStack());
		return style == null ? 1.0 : style.knockbackFactor(swing.attack().animation());
	}

	public static boolean launches(Player player, AttackHand swing) {
		WeaponStyle style = of(swing.itemStack());
		return style != null && style.launches(player, swing.attack().animation());
	}

	private static boolean shapesCooldown(ItemStack stack) {
		WeaponStyle style = of(stack);
		return style != null && style.shapesCooldown();
	}
}
