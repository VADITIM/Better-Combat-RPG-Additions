package dev.vadim.bcenchantments.client;

import dev.vadim.bcenchantments.RpgAdditions;
import dev.vadim.bcenchantments.combat.BetterCombatSwitch;
import dev.vadim.bcenchantments.combat.CriticalHits;
import dev.vadim.bcenchantments.network.ComboHudPayload;
import dev.vadim.bcenchantments.network.CritRollPayload;
import dev.vadim.bcenchantments.perk.Synergy;
import dev.vadim.bcenchantments.progression.Refinement;
import net.bettercombat.api.client.BetterCombatClientEvents;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;

public class RpgAdditionsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(ComboHudPayload.TYPE, (payload, context) -> ComboHud.update(payload));
		ClientPlayNetworking.registerGlobalReceiver(CritRollPayload.TYPE, (payload, context) -> CriticalHits.receive(payload));
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			// Both insert under the name, so the refinement lines end up above the synergies.
			Synergy.tooltip(stack, lines);
			Refinement.tooltip(stack, lines);
		});
		GuideScreen.register();
		ClientTickEvents.END_CLIENT_TICK.register(client -> BetterCombatSwitch.setDisabled(!HudConfig.get().betterCombat));
		HudElementRegistry.addLast(RpgAdditions.id("combo_meter"), new ComboHud());
		BetterCombatClientEvents.ATTACK_START.register((player, hand) -> {
			PunchyClips.onAttackStart(hand);
			PunchySpear.onAttackStart(player, hand);
		});
	}
}
