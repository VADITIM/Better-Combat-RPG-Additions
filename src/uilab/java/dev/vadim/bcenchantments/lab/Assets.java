package dev.vadim.bcenchantments.lab;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;

/** Minecraft's own textures, font and lang, read straight from the client jar Loom already downloaded. */
final class Assets {
	private final ZipFile jar;
	private final Map<String, BufferedImage> images = new HashMap<>();
	private final Map<String, Optional<BufferedImage>> items = new HashMap<>();

	private Assets(ZipFile jar) {
		this.jar = jar;
	}

	/** Opens {@code explicit} if given, else the newest {@code minecraft-client.jar} in Loom's cache. */
	static Assets open(String explicit) throws IOException {
		Path path = explicit != null ? Path.of(explicit) : find();
		if (path == null || !Files.exists(path)) {
			throw new IOException("No Minecraft client jar found. Run ./gradlew build once, or pass --jar <path>.");
		}
		System.out.println("Assets from " + path);
		return new Assets(new ZipFile(path.toFile()));
	}

	private static Path find() throws IOException {
		Path cache = Path.of(System.getProperty("user.home"), ".gradle", "caches", "fabric-loom");
		if (!Files.isDirectory(cache)) {
			return null;
		}
		try (Stream<Path> versions = Files.list(cache)) {
			return versions.map(p -> p.resolve("minecraft-client.jar")).filter(Files::exists)
				.max((a, b) -> Long.compare(a.toFile().lastModified(), b.toFile().lastModified())).orElse(null);
		}
	}

	String text(String path) {
		ZipEntry entry = jar.getEntry("assets/minecraft/" + path);
		if (entry == null) {
			return null;
		}
		try (InputStream in = jar.getInputStream(entry)) {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			return null;
		}
	}

	/** An image under {@code assets/minecraft/}, such as {@code textures/gui/container/smithing.png}; null if absent. */
	BufferedImage image(String path) {
		if (images.containsKey(path)) {
			return images.get(path);
		}
		BufferedImage image = null;
		ZipEntry entry = jar.getEntry("assets/minecraft/" + path);
		if (entry != null) {
			try (InputStream in = jar.getInputStream(entry)) {
				image = ImageIO.read(in);
			} catch (IOException e) {
				image = null;
			}
		}
		images.put(path, image);
		return image;
	}

	/** An item's sprite, the first frame if it is animated; null when the jar has none (shields, say). */
	BufferedImage item(String id) {
		return items.computeIfAbsent(id, key -> {
			BufferedImage image = image("textures/item/" + key + ".png");
			if (image == null) {
				image = image("textures/block/" + key + ".png");
			}
			if (image != null && image.getHeight() > image.getWidth()) {
				image = image.getSubimage(0, 0, image.getWidth(), image.getWidth());
			}
			return Optional.ofNullable(image);
		}).orElse(null);
	}
}
