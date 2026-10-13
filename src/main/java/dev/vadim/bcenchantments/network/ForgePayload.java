package dev.vadim.bcenchantments.network;

import dev.vadim.bcenchantments.TrimEnchantments;
import dev.vadim.bcenchantments.progression.Refinement;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** The weapon forge's request to the server: a weapon, and the trim powers it should carry in the order they are applied. */
public record ForgePayload(Identifier item, List<Refinement.Power> powers) implements CustomPacketPayload {
	public static final Type<ForgePayload> TYPE = new Type<>(TrimEnchantments.id("forge"));
	public static final StreamCodec<ByteBuf, ForgePayload> CODEC = StreamCodec.composite(
		Identifier.STREAM_CODEC, ForgePayload::item,
		ByteBufCodecs.fromCodec(Refinement.Power.CODEC.listOf()), ForgePayload::powers,
		ForgePayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
