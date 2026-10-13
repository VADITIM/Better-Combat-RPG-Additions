package dev.vadim.bcenchantments.client;

import dev.vadim.bcenchantments.TrimEnchantments;
import dev.vadim.bcenchantments.combat.BetterCombatSwitch;
import dev.vadim.bcenchantments.combat.CriticalHits;
import dev.vadim.bcenchantments.network.ComboHudPayload;
import dev.vadim.bcenchantments.network.CritRollPayload;
import net.bettercombat.api.client.BetterCombatClientEvents;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;

public class TrimEnchantmentsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(ComboHudPayload.TYPE, (payload, context) -> ComboHud.update(payload));
		ClientPlayNetworking.registerGlobalReceiver(CritRollPayload.TYPE, (payload, context) -> CriticalHits.receive(payload));
		PowerTooltips.register();
		GuideScreen.register();
		ClientTickEvents.END_CLIENT_TICK.register(client -> BetterCombatSwitch.setDisabled(!HudConfig.get().betterCombat));
		HudElementRegistry.addLast(TrimEnchantments.id("combo_meter"), new ComboHud());
		BetterCombatClientEvents.ATTACK_START.register((player, hand) -> {
			PunchyClips.onAttackStart(hand);
			PunchySpear.onAttackStart(player, hand);
		});
	}
}
