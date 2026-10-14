package dev.vadim.bcenchantments.network;

import dev.vadim.bcenchantments.TrimEnchantments;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** The smithing panel's request: spend one level point of the weapon on the table on {@code stat}. */
public record SpendPointPayload(String stat) implements CustomPacketPayload {
	public static final Type<SpendPointPayload> TYPE = new Type<>(TrimEnchantments.id("spend_point"));
	public static final StreamCodec<ByteBuf, SpendPointPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.STRING_UTF8, SpendPointPayload::stat,
		SpendPointPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
