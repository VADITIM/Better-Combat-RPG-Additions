package dev.vadim.bcenchantments.mixin;

import dev.vadim.bcenchantments.combat.ComboTracker;
import net.bettercombat.api.AttackHand;
import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.network.Packets;
import net.bettercombat.network.ServerNetwork;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Brackets one Better Combat swing on the server. Better Combat has no server-side hit event,
 * so this targets the lambda that runs a swing against its targets.
 */
@Mixin(ServerNetwork.class)
public abstract class ServerNetworkMixin {
	@Inject(method = "lambda$handleAttackRequest$2", at = @At("HEAD"))
	private static void bcenchantments$beginSwing(ServerPlayer player, WeaponAttributes attributes, WeaponAttributes.Attack attack, AttackHand hand, ServerLevel level, Packets.C2S_AttackRequest request, boolean useVanillaPacket, ServerGamePacketListenerImpl connection, boolean isSneaking, CallbackInfo ci) {
		ComboTracker.beginSwing(player, hand, request.comboCount(), request.cursorTarget());
	}

	@Inject(method = "lambda$handleAttackRequest$2", at = @At("RETURN"))
	private static void bcenchantments$endSwing(ServerPlayer player, WeaponAttributes attributes, WeaponAttributes.Attack attack, AttackHand hand, ServerLevel level, Packets.C2S_AttackRequest request, boolean useVanillaPacket, ServerGamePacketListenerImpl connection, boolean isSneaking, CallbackInfo ci) {
		ComboTracker.endSwing();
	}
}
