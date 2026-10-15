package dev.vadim.bcenchantments.combat;

import dev.vadim.bcenchantments.mob.Adaptation;
import dev.vadim.bcenchantments.network.ComboHudPayload;
import dev.vadim.bcenchantments.perk.ChargedPerk;
import dev.vadim.bcenchantments.perk.Perks;
import dev.vadim.bcenchantments.weapon.MaceTiming;
import java.util.ArrayList;
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
		long lastHitTick;
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
		MaceTiming.beginSwing(player, hand);
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
		MaceTiming.endSwing();
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
		long sinceHit = tick - streak.lastHitTick;
		int window = comboWindowTicks(player);
		boolean swapped = !own.weapon.isEmpty() && !ItemStack.isSameItem(weapon, own.weapon);
		streak.lastCombo = swingCombo;
		own.weapon = weapon.copy();
		own.attacks = hand.combo().total();

		if (!chained) {
			streak.combo = 0;
		}
		for (int i = 0; i < Perks.CHARGED.size(); i++) {
			int keep = Perks.CHARGED.get(i).keepTicks(window);
			boolean alive = chained || keep != ChargedPerk.BREAKS_WITH_COMBO && sinceHit <= keep;
			if (!alive) {
				streak.mainHand.charges[i] = 0;
				streak.offHand.charges[i] = 0;
			}
		}
		// A miss or a new weapon only costs the hand that swung; the other hand keeps its charge.
		if (hits.isEmpty() || swapped) {
			streak.combo = 0;
			for (int i = 0; i < Perks.CHARGED.size(); i++) {
				if (swapped || Perks.CHARGED.get(i).resetsOnMiss()) {
					own.charges[i] = 0;
				}
			}
		}
		if (hits.isEmpty()) {
			sync(player, streak, hand.isOffHand(), 0);
			return;
		}

		streak.combo++;
		streak.lastHitTick = tick;
		LivingEntity primary = hits.get(0);
		for (LivingEntity hit : hits) {
			// The cursor target wins over whatever else the sweep caught.
			if (hit.getId() == swingCursorTarget) {
				primary = hit;
			}
		}
		SwingResult swing = new SwingResult(player, hand, weapon, hits, primary);
		CombatHooks.swing(swing);
		sync(player, streak, hand.isOffHand(), charge(own, swing, null));
	}

	/**
	 * Counts an extra attack that landed outside a swing, such as an Echo, toward the hand's charged
	 * perks, leaving out {@code source}. It runs no swing effects, so no perk applies twice.
	 */
	public static void countExtraAttack(ServerPlayer player, AttackHand hand, List<LivingEntity> hits, ChargedPerk source) {
		Streak streak = STREAKS.get(player);
		if (streak == null || hits.isEmpty()) {
			return;
		}
		HandStreak own = hand.isOffHand() ? streak.offHand : streak.mainHand;
		if (!ItemStack.isSameItem(hand.itemStack(), own.weapon)) {
			return;
		}
		streak.lastHitTick = player.level().getGameTime();
		SwingResult extra = new SwingResult(player, hand, hand.itemStack(), hits, hits.get(0));
		sync(player, streak, hand.isOffHand(), charge(own, extra, source));
	}

	/**
	 * Adds {@code amount} to {@code perk}'s charge on the hand of {@code swing}, from a swing effect. The swing's
	 * own charging runs right after the swing effects, so a charge this fills fires with that swing.
	 */
	public static void addCharge(SwingResult swing, ChargedPerk perk, int amount) {
		Streak streak = STREAKS.computeIfAbsent(swing.player(), p -> new Streak());
		int index = Perks.CHARGED.indexOf(perk);
		if (index >= 0) {
			HandStreak own = swing.hand().isOffHand() ? streak.offHand : streak.mainHand;
			own.charges[index] += amount;
		}
	}

	/** Advances every charged perk but {@code skip} by {@code swing}, fires the full ones and returns their bits. */
	private static int charge(HandStreak own, SwingResult swing, ChargedPerk skip) {
		int fired = 0;
		for (int i = 0; i < Perks.CHARGED.size(); i++) {
			ChargedPerk perk = Perks.CHARGED.get(i);
			if (perk == skip) {
				continue;
			}
			int level = Perks.level(swing.weapon(), perk.enchantment());
			if (level <= 0) {
				own.charges[i] = 0;
				continue;
			}
			own.charges[i] = perk.advance(own.charges[i], swing);
			if (own.charges[i] >= perk.required(swing.weapon(), own.attacks)) {
				own.charges[i] = 0;
				perk.fire(swing, level);
				Adaptation.used(swing.player(), swing.weapon(), perk.enchantment());
				fired |= 1 << i;
			}
		}
		return fired;
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
			if (Perks.level(held, perk.enchantment()) <= 0) {
				charges.add(new ComboHudPayload.Charge(0, 0, (fired & 1 << i) != 0));
				continue;
			}
			// The charge is scaled onto the row's blocks, which may be fewer than the charge it needs.
			int required = perk.required(held, attacks);
			int segments = perk.segments(held, attacks);
			int value = same ? Math.min(own.charges[i], required) * segments / required : 0;
			charges.add(new ComboHudPayload.Charge(value, segments, (fired & 1 << i) != 0));
		}
		return charges;
	}

	private static int attackCount(ItemStack stack) {
		WeaponAttributes attributes = WeaponRegistry.getAttributes(stack);
		return attributes != null && attributes.attacks() != null ? attributes.attacks().length : 1;
	}
}
