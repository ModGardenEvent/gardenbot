package net.modgarden.gardenbot.modals.account;

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
import net.modgarden.gardenbot.interaction.modal.SimpleModal;
import net.modgarden.gardenbot.interaction.response.EmbedResponse;
import net.modgarden.gardenbot.interaction.response.Response;
import net.modgarden.gardenbot.util.ModGardenAPIClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class LinkModrinthModal extends SimpleModal {
	public LinkModrinthModal() {
		super("modalLinkModrinth", "Link your Modrinth account!", LinkModrinthModal::handleModal,
				ActionRow.of(
						TextInput.create("linkCode",
										"Link Code",
										TextInputStyle.SHORT
								).setMinLength(6)
								.setMaxLength(6).build()
				));
	}

	public static Response handleModal(ModalInteraction interaction) {
		User user = interaction.event().getUser();

		ModalMapping linkCode = interaction.event().getValue("linkCode");

		if (linkCode == null)
			return new EmbedResponse()
					.setTitle("Could not link your Modrinth account.")
					.setDescription("Link code is null.")
					.markEphemeral();

		JsonObject inputJson = new JsonObject();
		inputJson.addProperty("discord_id", user.getId());
		inputJson.addProperty("link_code", linkCode.getAsString());
		inputJson.addProperty("service", "modrinth");

		try {
			HttpResponse<InputStream> stream = ModGardenAPIClient.post(
					"discord/link",
					HttpRequest.BodyPublishers.ofString(inputJson.toString()),
					HttpResponse.BodyHandlers.ofInputStream(),
					"Authorization", "Basic " + GardenBot.DOTENV.get("OAUTH_SECRET"),
					"Content-Type", "application/json"
			);
			if (stream.statusCode() < 200 || stream.statusCode() > 299) {
				JsonElement json = JsonParser.parseReader(new InputStreamReader(stream.body()));
				String errorDescription = json.isJsonObject() && json.getAsJsonObject().has("description") ?
						json.getAsJsonObject().getAsJsonPrimitive("description").getAsString() :
						"Undefined Error.";
				if (stream.statusCode() == 422) {
					return new EmbedResponse()
							.setTitle("Could not link your Mod Garden account to Modrinth.")
							.setDescription(errorDescription)
							.setColor(0x5D3E40)
							.markEphemeral();
				}
				return new EmbedResponse()
						.setTitle("Encountered an exception whilst attempting to link your Mod Garden account to Modrinth.")
						.setDescription(stream.statusCode() + ": " + errorDescription + "\nPlease report this to a team member.")
						.setColor(0xFF0000)
						.markEphemeral();
			}
		} catch (IOException | InterruptedException ex) {
			GardenBot.LOG.error("", ex);
			return new EmbedResponse()
					.setTitle("Encountered an exception whilst attempting to link your Mod Garden account to Modrinth.")
					.setDescription(ex.getMessage() + "\nPlease report this to a team member.")
					.setColor(0xFF0000)
					.markEphemeral();
		}

		return new EmbedResponse()
				.setTitle("Successfully linked your Modrinth account to Mod Garden!")
				.setColor(0xA9FFA7)
				.markEphemeral();
	}
}
