package net.modgarden.gardenbot.modal.account;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.modgarden.gardenbot.GardenBot;
import net.modgarden.gardenbot.interaction.ModalInteraction;
import net.modgarden.gardenbot.modal.Modal;
import net.modgarden.gardenbot.response.EmbedResponse;
import net.modgarden.gardenbot.response.Response;
import net.modgarden.gardenbot.util.ModGardenAPIClient;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class RegisterModal extends Modal {
	public RegisterModal() {
		super(
				"modalRegister",
				"Register your Mod Garden account!",
				ActionRow.of(
						TextInput.create("username",
										"Username",
										TextInputStyle.SHORT
								)
								.setRequired(false)
								.setPlaceholder("Leave blank to use your Discord username.")
								.setMinLength(3)
								.setMaxLength(32).build()
				));
	}

	@NotNull
	@Override
	public Response respond(ModalInteraction interaction) {
		ModalMapping username = interaction.event().getValue("username");
		JsonElement createUserInput = createUserInput(
				username != null
						? username.getAsString()
						: interaction.event().getUser().getGlobalName()
		);

		try {
			HttpResponse<InputStream> postCreateUser = ModGardenAPIClient.post(
					"internal/user/create",
					HttpRequest.BodyPublishers.ofString(createUserInput.toString()),
					HttpResponse.BodyHandlers.ofInputStream(),
					"Content-Type", "application/json"
			);
			if (postCreateUser.statusCode() == 201) {
				String location = postCreateUser.headers()
						.firstValue("Location")
						.orElse(null);
				if (location == null) {
					throw new IOException("Unable to obtain user ID upon creating account");
				}
				String userId = location.substring("/v2/users/".length());

				JsonElement patchDiscordInput = modifyDiscordIntegrationInput(interaction.event().getUser().getId());
				HttpResponse<InputStream> patchDiscord = ModGardenAPIClient.patch(
						"internal/user/modify/" + userId,
						HttpRequest.BodyPublishers.ofString(patchDiscordInput.toString()),
						HttpResponse.BodyHandlers.ofInputStream(),
						"Content-Type", "application/json"
				);

				if (patchDiscord.statusCode() != 200) {
					JsonElement json = JsonParser.parseReader(new InputStreamReader(patchDiscord.body()));
					String errorDescription = json.isJsonObject() && json.getAsJsonObject().has("description") ?
							json.getAsJsonObject().getAsJsonPrimitive("description").getAsString() :
							"Undefined Error.";
					throw new IOException(errorDescription);
				}

				return new EmbedResponse()
						.setTitle("Your Mod Garden account has successfully been registered!")
						.setColor(0xA9FFA7)
						.markEphemeral();
			}

			JsonElement json = JsonParser.parseReader(new InputStreamReader(postCreateUser.body()));
			String errorDescription = json.isJsonObject() && json.getAsJsonObject().has("description") ?
					json.getAsJsonObject().getAsJsonPrimitive("description").getAsString() :
					"Undefined Error.";
			throw new IOException(errorDescription);
		} catch (IOException | InterruptedException ex) {
			GardenBot.LOG.error("", ex);
			return new EmbedResponse()
					.setTitle("Encountered an exception whilst attempting to register your Mod Garden account.")
					.setDescription(ex.getMessage() + "\nPlease report this to a team member.")
					.setColor(0xFF0000)
					.markEphemeral();
		}

	}

	private static JsonElement createUserInput(String username) {
		CreateUser data = new CreateUser();
		data.username = username;
		return GardenBot.GSON.toJsonTree(data, CreateUser.class);
	}

	private static JsonElement modifyDiscordIntegrationInput(String discordUserId) {
		ModifyUserData data = new ModifyUserData();
		data.integrations.discord.userId = discordUserId;
		return GardenBot.GSON.toJsonTree(data, ModifyUserData.class);
	}

	private static class CreateUser {
		public String username;
	}

	private static class ModifyUserData {
		public Integrations integrations = new Integrations();
	}

	private static class Integrations {
		public DiscordIntegration discord = new DiscordIntegration();
	}

	private static class DiscordIntegration {
		@SerializedName("user_id")
		private String userId;
	}
}
