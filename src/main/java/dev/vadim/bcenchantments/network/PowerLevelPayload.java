package dev.vadim.bcenchantments.network;

import dev.vadim.bcenchantments.TrimEnchantments;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** The smithing panel's request: raise ({@code up}) or lower the power of {@code trim} on the weapon on the table. */
public record PowerLevelPayload(String trim, boolean up) implements CustomPacketPayload {
	public static final Type<PowerLevelPayload> TYPE = new Type<>(TrimEnchantments.id("power_level"));
	public static final StreamCodec<ByteBuf, PowerLevelPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.STRING_UTF8, PowerLevelPayload::trim,
		ByteBufCodecs.BOOL, PowerLevelPayload::up,
		PowerLevelPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
