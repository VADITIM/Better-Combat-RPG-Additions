package dev.vadim.bcenchantments.network;

import dev.vadim.bcenchantments.RpgAdditions;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * The combo state after a swing, for the combo meter: the shared combo count and each hand's charges,
 * one per entry of {@code Perks.CHARGED} in the same order. A hand that shows nothing sends an empty list.
 */
public record ComboHudPayload(int combo, List<Charge> mainHand, List<Charge> offHand) implements CustomPacketPayload {
	public static final Type<ComboHudPayload> TYPE = new Type<>(RpgAdditions.id("combo_hud"));
	public static final StreamCodec<ByteBuf, ComboHudPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, ComboHudPayload::combo,
		Charge.LIST_CODEC, ComboHudPayload::mainHand,
		Charge.LIST_CODEC, ComboHudPayload::offHand,
		ComboHudPayload::new
	);

	/** One perk's charge on one hand. {@code max} is 0 when the weapon lacks the perk; {@code fired} when this swing set it off. */
	public record Charge(int value, int max, boolean fired) {
		public static final Charge NONE = new Charge(0, 0, false);
		public static final StreamCodec<ByteBuf, Charge> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, Charge::value,
			ByteBufCodecs.VAR_INT, Charge::max,
			ByteBufCodecs.BOOL, Charge::fired,
			Charge::new
		);
		public static final StreamCodec<ByteBuf, List<Charge>> LIST_CODEC = CODEC.apply(ByteBufCodecs.list());
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
