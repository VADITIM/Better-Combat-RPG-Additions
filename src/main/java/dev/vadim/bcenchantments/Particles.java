package dev.vadim.bcenchantments;

import dev.vadim.bcenchantments.network.ParticlePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Every particle this mod spawns goes through here instead of {@link ServerLevel#sendParticles}, with
 * the same arguments and the same 32-block reach. Clients with the mod get it as a
 * {@link ParticlePayload}, so their minimal-particles setting can thin it; clients without get the
 * plain vanilla packet.
 */
public final class Particles {
	private static final double RANGE = 32.0;

	private Particles() {
	}

	public static void send(ServerLevel world, ParticleOptions particle, double x, double y, double z, int count,
		double xDist, double yDist, double zDist, double speed) {
		ClientboundLevelParticlesPacket packet = new ClientboundLevelParticlesPacket(particle, false, false, x, y, z,
			(float) xDist, (float) yDist, (float) zDist, (float) speed, count);
		Vec3 at = new Vec3(x, y, z);
		for (ServerPlayer player : world.players()) {
			if (!player.blockPosition().closerToCenterThan(at, RANGE)) {
				continue;
			}
			if (ServerPlayNetworking.canSend(player, ParticlePayload.TYPE)) {
				ServerPlayNetworking.send(player, new ParticlePayload(packet));
			} else {
				player.connection.send(packet);
			}
		}
	}
}
