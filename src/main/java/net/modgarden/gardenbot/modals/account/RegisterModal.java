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
import net.modgarden.gardenbot.interaction.response.MessageResponse;
import net.modgarden.gardenbot.interaction.response.Response;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class RegisterModal extends SimpleModal {
	public RegisterModal() {
		super("modalRegister", "Register your Mod Garden account!", RegisterModal::handleModal,
				ActionRow.of(
						TextInput.create("username",
										"Username (Defaults to Discord Username)",
										TextInputStyle.SHORT
								)
								.setRequired(false)
								.setMaxLength(32).build()
				),
				ActionRow.of(
						TextInput.create("displayName",
										"Username (Defaults to Discord Display Name)",
										TextInputStyle.SHORT
								)
								.setRequired(false)
								.setMaxLength(32).build()
				));
	}

	public static Response handleModal(ModalInteraction interaction) {
		User user = interaction.event().getUser();
		String uri = GardenBot.API_URL + "discord/register";

		ModalMapping username = interaction.event().getValue("username");
		if (!username.getAsString().isEmpty() && !username.getAsString().matches(GardenBot.USERNAME_REGEX))
			return new EmbedResponse()
					.setTitle("Failed to register Mod Garden account.")
					.setDescription("Invalid characters in username.")
					.setColor(0X5D3E40)
					.markEphemeral();

		ModalMapping displayName = interaction.event().getValue("displayName");
		if (!username.getAsString().isEmpty() && !displayName.getAsString().matches(GardenBot.DISPLAY_NAME_REGEX))
			return new EmbedResponse()
					.setTitle("Failed to register Mod Garden account.")
					.setDescription("Invalid characters in display name.")
					.setColor(0X5D3E40)
					.markEphemeral();

		JsonObject inputJson = new JsonObject();
		inputJson.addProperty("id", user.getId());
		if (!username.getAsString().isEmpty())
			inputJson.addProperty("username", username.getAsString());
		if (!displayName.getAsString().isEmpty())
			inputJson.addProperty("display_name", displayName.getAsString());

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
						.setTitle("Encountered an exception whilst attempting to register your Mod Garden account.")
						.setDescription(stream.statusCode() + ": " + errorDescription + "\nPlease report this to a team member.")
						.setColor(0xFF0000)
						.markEphemeral();
			}
		} catch (IOException | InterruptedException ex) {
			GardenBot.LOG.error("", ex);
		}

		return new EmbedResponse()
				.setTitle("Your Mod Garden account has successfully been registered!")
				.setColor(0xA9FFA7)
				.markEphemeral();
	}
}
