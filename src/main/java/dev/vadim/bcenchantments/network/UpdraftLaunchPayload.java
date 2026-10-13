package dev.vadim.bcenchantments.network;

import dev.vadim.bcenchantments.TrimEnchantments;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Tells the client Updraft just launched its player, which opens the spear's air lunge until it lands. */
public record UpdraftLaunchPayload() implements CustomPacketPayload {
	public static final UpdraftLaunchPayload INSTANCE = new UpdraftLaunchPayload();
	public static final Type<UpdraftLaunchPayload> TYPE = new Type<>(TrimEnchantments.id("updraft_launch"));
	public static final StreamCodec<ByteBuf, UpdraftLaunchPayload> CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
