package dev.vadim.bcenchantments.combat;

import dev.vadim.bcenchantments.network.ComboHudPayload;
import dev.vadim.bcenchantments.perk.ChargedPerk;
import dev.vadim.bcenchantments.perk.Perks;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.bettercombat.BetterCombatMod;
import net.bettercombat.api.AttackHand;
import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.logic.PlayerAttackHelper;
import net.bettercombat.logic.WeaponRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Follows Better Combat swings on the server and keeps each player's combo: consecutive swings that
 * landed at least one hit. A miss, a pause long enough for Better Combat to restart its combo, or a
 * weapon swap breaks it. Each hand keeps its own charge toward every {@link ChargedPerk}, so two
 * dual-wielded weapons charge separately. Everything else that reacts to a swing hangs off
 * {@link CombatHooks}.
 */
public final class ComboTracker {
	/** One hand's own progress: dual-wielded weapons charge their perks separately. */
	private static final class HandStreak {
		ItemStack weapon = ItemStack.EMPTY;
		int attacks;
		final int[] charges = new int[Perks.CHARGED.size()];
	}

	private static final class Streak {
		int lastCombo = Integer.MIN_VALUE;
		long lastSwingTick;
		int combo;
		final HandStreak mainHand = new HandStreak();
		final HandStreak offHand = new HandStreak();
	}

	private static final Map<ServerPlayer, Streak> STREAKS = new WeakHashMap<>();

	// Swings run synchronously on the server thread, so one in-flight swing at a time is enough.
	private static ServerPlayer swingPlayer;
	private static AttackHand swingHand;
	private static int swingCombo;
	private static int swingCursorTarget;
	private static final List<LivingEntity> swingHits = new ArrayList<>();

	private static float healthBefore;
	private static ArmorStand struckStand;

	private ComboTracker() {
	}

	public static void beginSwing(ServerPlayer player, AttackHand hand, int comboCount, int cursorTarget) {
		swingPlayer = player;
		swingHand = hand;
		swingCombo = comboCount;
		swingCursorTarget = cursorTarget;
		swingHits.clear();
		CriticalHits.beginSwing(player, hand.isOffHand());
	}

	public static void beforeAttack(ServerPlayer player, Entity target) {
		struckStand = null;
		if (target instanceof LivingEntity living) {
			healthBefore = living.getHealth() + living.getAbsorptionAmount();
		}
	}

	/** Armor stands never lose health, so their unbreakable mixin reports a swing on them here instead. */
	public static void armorStandHit(ArmorStand stand, DamageSource source) {
		if (source.getEntity() instanceof Player) {
			struckStand = stand;
		}
	}

	public static void afterAttack(ServerPlayer player, Entity target) {
		if (!(target instanceof LivingEntity living)) {
			return;
		}
		boolean landed = living == struckStand
			|| living.isDeadOrDying()
			|| living.getHealth() + living.getAbsorptionAmount() < healthBefore;
		struckStand = null;
		if (!landed) {
			return;
		}
		if (player == swingPlayer) {
			if (!swingHits.contains(living)) {
				swingHits.add(living);
			}
		} else {
			// A plain vanilla attack outside a Better Combat swing is always a direct hit.
			CombatHooks.directHit(player, player.getMainHandItem(), living);
		}
	}

	/** The Better Combat swing {@code attacker} is running on the server right now, or null. */
	public static AttackHand currentSwing(Entity attacker) {
		return attacker != null && attacker == swingPlayer ? swingHand : null;
	}

	public static void endSwing() {
		ServerPlayer player = swingPlayer;
		AttackHand hand = swingHand;
		List<LivingEntity> hits = List.copyOf(swingHits);
		swingPlayer = null;
		swingHand = null;
		swingHits.clear();
		if (player == null || hand == null) {
			return;
		}
		CriticalHits.endSwing(player, hand.isOffHand());

		Streak streak = STREAKS.computeIfAbsent(player, p -> new Streak());
		HandStreak own = hand.isOffHand() ? streak.offHand : streak.mainHand;
		ItemStack weapon = hand.itemStack();
		// Better Combat counts swings of both hands up together and starts over at 0 after a pause, so a gap means the combo broke.
		boolean chained = swingCombo == streak.lastCombo + 1;
		long tick = player.level().getGameTime();
		long paused = tick - streak.lastSwingTick;
		int window = comboWindowTicks(player);
		boolean swapped = !own.weapon.isEmpty() && !ItemStack.isSameItem(weapon, own.weapon);
		streak.lastSwingTick = tick;
		streak.lastCombo = swingCombo;
		own.weapon = weapon.copy();
		own.attacks = hand.combo().total();

		if (!chained) {
			streak.combo = 0;
		}
		for (int i = 0; i < Perks.CHARGED.size(); i++) {
			int grace = Perks.CHARGED.get(i).graceTicks();
			boolean alive = chained || grace != ChargedPerk.BREAKS_WITH_COMBO && paused <= window + grace;
			if (!alive) {
				streak.mainHand.charges[i] = 0;
				streak.offHand.charges[i] = 0;
			}
		}
		// A miss or a new weapon only costs the hand that swung; the other hand keeps its charge.
		if (hits.isEmpty() || swapped) {
			streak.combo = 0;
			Arrays.fill(own.charges, 0);
		}
		if (hits.isEmpty()) {
			sync(player, streak, hand.isOffHand(), 0);
			return;
		}

		streak.combo++;
		LivingEntity primary = hits.get(0);
		for (LivingEntity hit : hits) {
			// The cursor target wins over whatever else the sweep caught.
			if (hit.getId() == swingCursorTarget) {
				primary = hit;
			}
		}
		SwingResult swing = new SwingResult(player, hand, weapon, hits, primary);
		CombatHooks.swing(swing);

		int fired = 0;
		for (int i = 0; i < Perks.CHARGED.size(); i++) {
			ChargedPerk perk = Perks.CHARGED.get(i);
			int level = Perks.level(weapon, perk.enchantment());
			if (level <= 0) {
				own.charges[i] = 0;
				continue;
			}
			own.charges[i] = perk.advance(own.charges[i], swing);
			if (own.charges[i] >= perk.required(own.attacks)) {
				own.charges[i] = 0;
				perk.fire(swing, level);
				fired |= 1 << i;
			}
		}
		sync(player, streak, hand.isOffHand(), fired);
	}

	/** How long Better Combat waits after a swing before it drops the combo, worked out the way its client does. */
	private static int comboWindowTicks(ServerPlayer player) {
		return Math.round(PlayerAttackHelper.getAttackCooldownTicksCapped(player) * BetterCombatMod.config.combo_reset_rate);
	}

	private static void sync(ServerPlayer player, Streak streak, boolean offHandSwung, int fired) {
		if (!ServerPlayNetworking.canSend(player, ComboHudPayload.TYPE)) {
			return;
		}
		List<ComboHudPayload.Charge> mainHand = charges(streak.mainHand, player.getMainHandItem(), offHandSwung ? 0 : fired);
		List<ComboHudPayload.Charge> offHand = PlayerAttackHelper.isDualWielding(player)
			? charges(streak.offHand, player.getOffhandItem(), offHandSwung ? fired : 0)
			: List.of();
		ServerPlayNetworking.send(player, new ComboHudPayload(streak.combo, mainHand, offHand));
	}

	/** What the HUD shows for one hand: one entry per charged perk, measured against the weapon it holds now. */
	private static List<ComboHudPayload.Charge> charges(HandStreak own, ItemStack held, int fired) {
		boolean same = ItemStack.isSameItem(held, own.weapon);
		int attacks = same ? own.attacks : attackCount(held);
		List<ComboHudPayload.Charge> charges = new ArrayList<>(Perks.CHARGED.size());
		for (int i = 0; i < Perks.CHARGED.size(); i++) {
			ChargedPerk perk = Perks.CHARGED.get(i);
			int max = Perks.level(held, perk.enchantment()) > 0 ? perk.required(attacks) : 0;
			charges.add(new ComboHudPayload.Charge(same && max > 0 ? own.charges[i] : 0, max, (fired & 1 << i) != 0));
		}
		return charges;
	}

	private static int attackCount(ItemStack stack) {
		WeaponAttributes attributes = WeaponRegistry.getAttributes(stack);
		return attributes != null && attributes.attacks() != null ? attributes.attacks().length : 1;
	}
}
