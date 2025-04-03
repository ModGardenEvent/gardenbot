package net.modgarden.gardenbot.interaction.modal;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.modgarden.gardenbot.GardenBot;
import net.modgarden.gardenbot.interaction.ModalInteraction;
import net.modgarden.gardenbot.interaction.response.EmbedResponse;
import net.modgarden.gardenbot.interaction.response.MessageResponse;
import net.modgarden.gardenbot.interaction.response.Response;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class LinkModrinthModal extends SimpleModal {
	public LinkModrinthModal() {
		super("modalLinkModrinth", "Link your Modrinth account!", LinkModrinthModal::handleModal,
				ActionRow.of(
						TextInput.create("linkCode",
										"LinK Code",
										TextInputStyle.SHORT
								).setMinLength(6)
								.setMaxLength(6).build()
				));
	}


	public static Response handleModal(ModalInteraction interaction) {
		User user = interaction.event().getUser();
		String uri = GardenBot.API_URL + "link/discord";

		ModalMapping linkCode = interaction.event().getValue("linkCode");

		if (linkCode == null)
			return new EmbedResponse()
					.setTitle("Failed to link Modrinth account.")
					.setDescription("Link code is null.")
					.markEphemeral();

		JsonObject inputJson = new JsonObject();
		inputJson.addProperty("discord_id", user.getId());
		inputJson.addProperty("link_code", linkCode.getAsString());
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
						.setTitle("Failed to register Mod Garden account.")
						.setDescription(stream.statusCode() + ": " + errorDescription)
						.markEphemeral();
			}
		} catch (IOException | InterruptedException ex) {
			GardenBot.LOG.error("", ex);
		}

		return new MessageResponse()
				.setMessage("Successfully linked your Modrinth account to Mod Garden!")
				.markEphemeral();
	}
}
