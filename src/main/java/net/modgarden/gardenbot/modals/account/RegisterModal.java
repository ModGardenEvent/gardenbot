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
import java.util.Objects;

public class RegisterModal extends SimpleModal {
	public RegisterModal() {
		super("modalRegister", "Register your Mod Garden account!", RegisterModal::handleModal,
				ActionRow.of(
						TextInput.create("username",
										"Username",
										TextInputStyle.SHORT
								)
								.setRequired(false)
								.setPlaceholder("Leave blank to use your Discord username.")
								.setMaxLength(32).build()
				),
				ActionRow.of(
						TextInput.create("displayName",
										"Display Name",
										TextInputStyle.SHORT
								)
								.setRequired(false)
								.setPlaceholder("Leave blank to use your Discord display name.")
								.setMaxLength(32).build()
				));
	}

	public static Response handleModal(ModalInteraction interaction) {
		User user = interaction.event().getUser();

		ModalMapping username = Objects.requireNonNull(interaction.event().getValue("username"));
		if (!username.getAsString().isEmpty() && !username.getAsString().matches(GardenBot.USERNAME_REGEX))
			return new EmbedResponse()
					.setTitle("Could not register your Mod Garden account.")
					.setDescription("Invalid characters in username.")
					.setColor(0X5D3E40)
					.markEphemeral();

		ModalMapping displayName = Objects.requireNonNull(interaction.event().getValue("displayName"));
		if (!displayName.getAsString().isEmpty() && !displayName.getAsString().matches(GardenBot.DISPLAY_NAME_REGEX))
			return new EmbedResponse()
					.setTitle("Could not register your Mod Garden account.")
					.setDescription("Invalid characters in display name.")
					.setColor(0X5D3E40)
					.markEphemeral();

		JsonObject inputJson = new JsonObject();
		inputJson.addProperty("id", user.getId());
		if (!username.getAsString().isEmpty())
			inputJson.addProperty("username", username.getAsString());
		if (!displayName.getAsString().isEmpty())
			inputJson.addProperty("display_name", displayName.getAsString());

		try {
			HttpResponse<InputStream> stream = ModGardenAPIClient.post(
					"discord/register",
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
				return new EmbedResponse()
						.setTitle("Encountered an exception whilst attempting to register your Mod Garden account.")
						.setDescription(stream.statusCode() + ": " + errorDescription + "\nPlease report this to a team member.")
						.setColor(0xFF0000)
						.markEphemeral();
			}
		} catch (IOException | InterruptedException ex) {
			GardenBot.LOG.error("", ex);
			return new EmbedResponse()
					.setTitle("Encountered an exception whilst attempting to register your Mod Garden account.")
					.setDescription(ex.getMessage() + "\nPlease report this to a team member.")
					.setColor(0xFF0000)
					.markEphemeral();
		}

		return new EmbedResponse()
				.setTitle("Your Mod Garden account has successfully been registered!")
				.setColor(0xA9FFA7)
				.markEphemeral();
	}
}
