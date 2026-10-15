package dev.vadim.bcenchantments.network;

import dev.vadim.bcenchantments.TrimEnchantments;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * The landmark the player now stands in, for the inventory's difficulty panel: the ordinal of a
 * {@code mob.Landmark}, or -1 outside every one. Sent only when it changes; the client cannot see
 * structures itself.
 */
public record LandmarkPayload(int landmark) implements CustomPacketPayload {
	public static final Type<LandmarkPayload> TYPE = new Type<>(TrimEnchantments.id("landmark"));
	public static final StreamCodec<ByteBuf, LandmarkPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, LandmarkPayload::landmark,
		LandmarkPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
