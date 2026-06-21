package net.modgarden.gardenbot.util.loader;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;
import net.modgarden.gardenbot.GardenBot;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public record FabricModJson(@SerializedName("id") String modId,
							String name,
							String version) {
	public static FabricModJson getFabricModJson(File file) throws IllegalStateException {
		try {

			FabricModJson fabricModJson;
			try (
					JarFile jarFile = new JarFile(file);
					InputStream fmjStream = getFmjAsStream(jarFile);
					InputStreamReader fmjStreamReader = new InputStreamReader(fmjStream)
			) {
				JsonElement potentialFmj = JsonParser.parseReader(fmjStreamReader);
				if (!potentialFmj.isJsonObject()) {
					throw new IllegalStateException("Attempted to get a non-JSONObject fabric.mod.json whilst getting project metadata.");
				}

				JsonObject fmj = potentialFmj.getAsJsonObject();

				fabricModJson = GardenBot.GSON.fromJson(fmj, FabricModJson.class);
			}

			return fabricModJson;
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	public static boolean isFabricMod(File file) throws IOException {
		try (JarFile jarFile = new JarFile(file)) {
			return jarFile.getEntry("fabric.mod.json") != null;
		}
	}

	private static InputStream getFmjAsStream(JarFile file) throws IOException {
		ZipEntry entry = file.getEntry("fabric.mod.json");
		if (entry != null) {
			return file.getInputStream(entry);
		}
		throw new IllegalStateException("The specified JAR is not a Fabric mod.");
	}
}
