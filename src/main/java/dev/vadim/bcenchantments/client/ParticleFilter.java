package dev.vadim.bcenchantments.client;

import dev.vadim.bcenchantments.network.ParticlePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * Plays the mod's particles ({@link ParticlePayload}) through vanilla's own handler, thinned first when
 * the player picked minimal particles: every burst keeps about a fifth of its particles (at least one),
 * single particles laid in lines and rings keep about one in three, and a gust emitter, which spawns a
 * whole cloud of gusts, becomes one small gust. Every effect still shows a little.
 */
public final class ParticleFilter {
	private static final float BURST_SHARE = 0.2F;
	private static final float SINGLE_CHANCE = 0.33F;
	private static final RandomSource RANDOM = RandomSource.create();

	private ParticleFilter() {
	}

	public static void receive(ParticlePayload payload) {
		ClientPacketListener connection = Minecraft.getInstance().getConnection();
		if (connection == null) {
			return;
		}
		ClientboundLevelParticlesPacket packet = payload.packet();
		if (HudConfig.get().minimalParticles) {
			packet = thin(packet);
			if (packet == null) {
				return;
			}
		}
		connection.handleParticleEvent(packet);
	}

	private static ClientboundLevelParticlesPacket thin(ClientboundLevelParticlesPacket packet) {
		ParticleOptions particle = packet.getParticle();
		int count = packet.getCount();
		if (particle.getType() == ParticleTypes.GUST_EMITTER_SMALL || particle.getType() == ParticleTypes.GUST_EMITTER_LARGE) {
			particle = ParticleTypes.SMALL_GUST;
			count = 1;
		} else if (particle.getType() == ParticleTypes.EXPLOSION) {
			return packet;
		} else if (count > 1) {
			count = Math.max(1, Mth.ceil(count * BURST_SHARE));
		} else if (RANDOM.nextFloat() >= SINGLE_CHANCE) {
			return null;
		}
		return new ClientboundLevelParticlesPacket(particle, packet.isOverrideLimiter(), packet.alwaysShow(),
			packet.getX(), packet.getY(), packet.getZ(), packet.getXDist(), packet.getYDist(), packet.getZDist(),
			packet.getMaxSpeed(), count);
	}
}
