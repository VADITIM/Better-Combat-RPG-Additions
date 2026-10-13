package dev.vadim.bcenchantments.network;

import dev.vadim.bcenchantments.TrimEnchantments;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;

/**
 * A vanilla particle packet sent by this mod, wrapped so the client knows it is ours and can thin it
 * out when the player picks minimal particles ({@code client/ParticleFilter}).
 */
public record ParticlePayload(ClientboundLevelParticlesPacket packet) implements CustomPacketPayload {
	public static final Type<ParticlePayload> TYPE = new Type<>(TrimEnchantments.id("particles"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ParticlePayload> CODEC =
		ClientboundLevelParticlesPacket.STREAM_CODEC.map(ParticlePayload::new, ParticlePayload::packet);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
