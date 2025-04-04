package net.modgarden.gardenbot;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.dv8tion.jda.api.entities.User;
import net.modgarden.gardenbot.interaction.ButtonInteraction;
import net.modgarden.gardenbot.interaction.button.ButtonDispatcher;
import net.modgarden.gardenbot.interaction.response.EmbedResponse;
import net.modgarden.gardenbot.interaction.response.MessageResponse;
import net.modgarden.gardenbot.interaction.response.ModalResponse;
import net.modgarden.gardenbot.interaction.response.Response;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class GardenBotButtonHandlers {
	public static void registerAll() {
		ButtonDispatcher.register("linkModrinth", new ModalResponse(GardenBotModals.LINK_MODRINTH));
		ButtonDispatcher.register("unlinkModrinth", GardenBotButtonHandlers::unlinkModrinth);
	}

	public static Response unlinkModrinth(ButtonInteraction interaction) {
		User user = interaction.event().getUser();
		String uri = GardenBot.API_URL + "unlink/discord";

		JsonObject inputJson = new JsonObject();
		inputJson.addProperty("discord_id", user.getId());
		inputJson.addProperty("service", "modrinth");

		var req = HttpRequest.newBuilder(URI.create(uri))
				.headers(
						"Authorization", "Basic " + GardenBot.DOTENV.get("OAUTH_SECRET"),
						"Content-Type", "application/json"
				)
				.POST(HttpRequest.BodyPublishers.ofString(inputJson.toString()))
				.build();

		try {
			HttpResponse<InputStream> stream = GardenBot.HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofInputStream());
			if (stream.statusCode() < 200 || stream.statusCode() > 299) {
				JsonElement json = JsonParser.parseReader(new InputStreamReader(stream.body()));
				String errorDescription = json.isJsonObject() && json.getAsJsonObject().has("description") ?
						json.getAsJsonObject().getAsJsonPrimitive("description").getAsString() :
						"Undefined Error.";
				return new EmbedResponse()
						.setTitle("Failed to unlink Mod Garden account.")
						.setDescription(stream.statusCode() + ": " + errorDescription + "\nPlease report this to a team member.")
						.setColor(0xFF0000)
						.markEphemeral();
			}
		} catch (IOException | InterruptedException ex) {
			GardenBot.LOG.error("", ex);
		}

		return new MessageResponse()
				.setMessage("Sucessfully unlinked your Modrinth account from Mod Garden!")
				.markEphemeral();
	}
}
