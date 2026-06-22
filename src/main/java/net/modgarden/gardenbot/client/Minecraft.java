package net.modgarden.gardenbot.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import net.modgarden.gardenbot.GardenBot;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static net.modgarden.gardenbot.GardenBot.HTTP_CLIENT;

public class Minecraft {
	private static final String API_URL = "https://api.minecraftservices.com/";
	private static final String USER_AGENT = "ModGardenEvent/gardenbot/" + GardenBot.VERSION + " (modgarden.net)";

	@Nullable
	public static String getMinecraftUsernameFromUuid(String uuid) {
		try {
			HttpResponse<InputStream> response = get("minecraft/profile/lookup/" + uuid, HttpResponse.BodyHandlers.ofInputStream());
			try (var reader = new InputStreamReader(response.body())) {
				JsonElement json = JsonParser.parseReader(reader);
				if (json.isJsonObject()) {
					JsonPrimitive primitive = json.getAsJsonObject().getAsJsonPrimitive("name");
					if (primitive != null && primitive.isString())
						return primitive.getAsString();
				}
			}
		} catch (IOException | InterruptedException e) {
			GardenBot.LOG.error("Failed to get Minecraft username from UUID.", e);
		}
		return null;
	}

	@Nullable
	public static String getUuidFromMinecraftUsername(String username) {
		try {
			HttpResponse<InputStream> response = get("minecraft/profile/lookup/name/" + username, HttpResponse.BodyHandlers.ofInputStream());
			try (var reader = new InputStreamReader(response.body())) {
				JsonElement json = JsonParser.parseReader(reader);
				if (json.isJsonObject()) {
					JsonPrimitive primitive = json.getAsJsonObject().getAsJsonPrimitive("id");
					if (primitive != null && primitive.isString())
						return primitive.getAsString();
				}
			}
		} catch (IOException | InterruptedException e) {
			GardenBot.LOG.error("Failed to get Minecraft username from UUID.", e);
		}
		return null;
	}

	private static <T> HttpResponse<T> get(String endpoint, HttpResponse.BodyHandler<T> bodyHandler, String... headers) throws IOException, InterruptedException {
		var req = HttpRequest.newBuilder(URI.create(API_URL + endpoint))
				.header("User-Agent", USER_AGENT);
		if (headers.length > 0) {
			req.headers(headers);
		}

		return HTTP_CLIENT.send(req.build(), bodyHandler);
	}
}
