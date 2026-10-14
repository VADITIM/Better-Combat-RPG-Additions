package dev.vadim.bcenchantments.network;

import dev.vadim.bcenchantments.TrimEnchantments;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Sent as a mace swing starts: whether the player started it inside the Perfect Hit window. */
public record MaceTimingPayload(boolean perfect) implements CustomPacketPayload {
	public static final Type<MaceTimingPayload> TYPE = new Type<>(TrimEnchantments.id("mace_timing"));
	public static final StreamCodec<ByteBuf, MaceTimingPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.BOOL, MaceTimingPayload::perfect,
		MaceTimingPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
