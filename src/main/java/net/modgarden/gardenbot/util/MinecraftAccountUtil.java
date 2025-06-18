package net.modgarden.gardenbot.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import net.modgarden.gardenbot.GardenBot;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class MinecraftAccountUtil {
	@Nullable
	public static String getMinecraftUsernameFromUuid(String uuid) {
		HttpRequest nameRequest = HttpRequest.newBuilder(URI.create(
				"https://api.minecraftservices.com/minecraft/profile/lookup/" + uuid
		)).build();

		try {
			var response = GardenBot.HTTP_CLIENT.send(nameRequest, HttpResponse.BodyHandlers.ofInputStream());
			try (var reader = new InputStreamReader(response.body())) {
				JsonElement json = JsonParser.parseReader(reader);
				if (json.isJsonObject()) {
					JsonPrimitive primitive = json.getAsJsonObject().getAsJsonPrimitive("name");
					if (primitive != null && primitive.isString())
						return primitive.getAsString();
				}
			}
		} catch (IOException | InterruptedException ex) {
			GardenBot.LOG.error("Failed to get Minecraft username from UUID.", ex);
		}
		return null;
	}

	@Nullable
	public static String getUuidFromMinecraftUsername(String username) {
		HttpRequest nameRequest = HttpRequest.newBuilder(URI.create(
				"https://api.minecraftservices.com/minecraft/profile/lookup/name/" + username
		)).build();

		try {
			var response = GardenBot.HTTP_CLIENT.send(nameRequest, HttpResponse.BodyHandlers.ofInputStream());
			try (var reader = new InputStreamReader(response.body())) {
				JsonElement json = JsonParser.parseReader(reader);
				if (json.isJsonObject()) {
					JsonPrimitive primitive = json.getAsJsonObject().getAsJsonPrimitive("id");
					if (primitive != null && primitive.isString())
						return primitive.getAsString();
				}
			}
		} catch (IOException | InterruptedException ex) {
			GardenBot.LOG.error("Failed to get Minecraft username from UUID.", ex);
		}
		return null;
	}
}
