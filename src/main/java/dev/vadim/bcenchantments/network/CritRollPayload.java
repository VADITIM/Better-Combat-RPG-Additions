package dev.vadim.bcenchantments.network;

import dev.vadim.bcenchantments.TrimEnchantments;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** The rolls waiting for the player's next swing with each hand; a swing crits when its roll is under the weapon's critical chance. */
public record CritRollPayload(float mainHand, float offHand) implements CustomPacketPayload {
	public static final Type<CritRollPayload> TYPE = new Type<>(TrimEnchantments.id("crit_roll"));
	public static final StreamCodec<ByteBuf, CritRollPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.FLOAT, CritRollPayload::mainHand,
		ByteBufCodecs.FLOAT, CritRollPayload::offHand,
		CritRollPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
