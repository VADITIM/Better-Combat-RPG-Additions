package dev.vadim.bcenchantments.network;

import dev.vadim.bcenchantments.TrimEnchantments;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** The client's right click in the air for Updraft's air move, and which hand's weapon starts it. */
public record AirMovePayload(boolean offHand) implements CustomPacketPayload {
	public static final Type<AirMovePayload> TYPE = new Type<>(TrimEnchantments.id("air_move"));
	public static final StreamCodec<ByteBuf, AirMovePayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.BOOL, AirMovePayload::offHand,
		AirMovePayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
