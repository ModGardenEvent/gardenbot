package net.modgarden.gardenbot.commands.account;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.modgarden.gardenbot.GardenBot;
import net.modgarden.gardenbot.interaction.SlashCommandInteraction;
import net.modgarden.gardenbot.interaction.response.EmbedResponse;
import net.modgarden.gardenbot.interaction.response.MessageResponse;
import net.modgarden.gardenbot.interaction.response.Response;
import net.modgarden.gardenbot.util.ModGardenAPIClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.stream.Collectors;

public class ProfileCommandHandler {
	public static Response handleModifyUsername(SlashCommandInteraction interaction) {
		User user = interaction.event().getUser();

		try {
			HttpResponse<InputStream> userStream = ModGardenAPIClient.get("user/" + user.getId() + "?service=discord", HttpResponse.BodyHandlers.ofInputStream());
			if (userStream.statusCode() == 404) {
				return new MessageResponse()
						.setMessage("You do not have a Mod Garden account.\nPlease create one with **/account create**.")
						.markEphemeral();
			} else if (userStream.statusCode() != 200) {
				JsonElement json = JsonParser.parseReader(new InputStreamReader(userStream.body()));
				String errorDescription = json.isJsonObject() && json.getAsJsonObject().has("description") ?
						json.getAsJsonObject().getAsJsonPrimitive("description").getAsString() :
						"Undefined Error.";
				return new EmbedResponse()
						.setTitle("Encountered an exception whilst attempting to modify your username.")
						.setDescription(userStream.statusCode() + ": " + errorDescription + "\nPlease report this to a team member.")
						.setColor(0xFF0000)
						.markEphemeral();
			}


			OptionMapping value = Objects.requireNonNull(interaction.event().getOption("username"));
			JsonObject inputJson = new JsonObject();
			inputJson.addProperty("discord_id", user.getId());
			inputJson.addProperty("value", value.getAsString());

			HttpResponse<InputStream> postStream = ModGardenAPIClient.post(
					"discord/modify/username",
					HttpRequest.BodyPublishers.ofString(inputJson.toString()),
					HttpResponse.BodyHandlers.ofInputStream(),
					"Authorization", "Basic " + GardenBot.DOTENV.get("OAUTH_SECRET"),
					"Content-Type", "application/json"
			);
			if (postStream.statusCode() < 200 || postStream.statusCode() > 299) {
				JsonElement json = JsonParser.parseReader(new InputStreamReader(postStream.body()));
				String errorDescription = json.isJsonObject() && json.getAsJsonObject().has("description") ?
						json.getAsJsonObject().getAsJsonPrimitive("description").getAsString() :
						"Undefined Error.";
				if (postStream.statusCode() == 422) {
					return new EmbedResponse()
							.setTitle("Could not change your username.")
							.setDescription(errorDescription)
							.setColor(0x5D3E40)
							.markEphemeral();
				}
				return new EmbedResponse()
						.setTitle("Encountered an exception whilst attempting to modify your username.")
						.setDescription(postStream.statusCode() + ": " + errorDescription + "\nPlease report this to a team member.")
						.setColor(0xFF0000)
						.markEphemeral();
			}

			String title = new BufferedReader(
					new InputStreamReader(postStream.body(), StandardCharsets.UTF_8)
			).lines().collect(Collectors.joining("\n"));

			return new EmbedResponse()
					.setTitle(title)
					.setColor(0xA9FFA7)
					.markEphemeral();
		} catch (IOException | InterruptedException ex) {
			GardenBot.LOG.error("", ex);
			return new EmbedResponse()
					.setTitle("Encountered an exception whilst attempting to modify your username.")
					.setDescription(ex.getMessage() + "\nPlease report this to a team member.")
					.setColor(0xFF0000)
					.markEphemeral();
		}
	}

	public static Response handleModifyDisplayName(SlashCommandInteraction interaction) {
		User user = interaction.event().getUser();

		try {
			HttpResponse<InputStream> userStream = ModGardenAPIClient.get("user/" + user.getId() + "?service=discord", HttpResponse.BodyHandlers.ofInputStream());
			if (userStream.statusCode() == 404) {
				return new MessageResponse()
						.setMessage("You do not have a Mod Garden account.\nPlease create one with **/account create**.")
						.markEphemeral();
			} else if (userStream.statusCode() != 200) {
				JsonElement json = JsonParser.parseReader(new InputStreamReader(userStream.body()));
				String errorDescription = json.isJsonObject() && json.getAsJsonObject().has("description") ?
						json.getAsJsonObject().getAsJsonPrimitive("description").getAsString() :
						"Undefined Error.";
				return new EmbedResponse()
						.setTitle("Encountered an exception whilst attempting to modify your display name.")
						.setDescription(userStream.statusCode() + ": " + errorDescription + "\nPlease report this to a team member.")
						.setColor(0xFF0000)
						.markEphemeral();
			}


			OptionMapping value = Objects.requireNonNull(interaction.event().getOption("displayname"));
			JsonObject inputJson = new JsonObject();
			inputJson.addProperty("discord_id", user.getId());
			inputJson.addProperty("value", value.getAsString());

			HttpResponse<InputStream> postStream = ModGardenAPIClient.post(
					"discord/modify/displayname",
					HttpRequest.BodyPublishers.ofString(inputJson.toString()),
					HttpResponse.BodyHandlers.ofInputStream(),
					"Authorization", "Basic " + GardenBot.DOTENV.get("OAUTH_SECRET"),
					"Content-Type", "application/json"
			);
			if (postStream.statusCode() < 200 || postStream.statusCode() > 299) {
				JsonElement json = JsonParser.parseReader(new InputStreamReader(postStream.body()));
				String errorDescription = json.isJsonObject() && json.getAsJsonObject().has("description") ?
						json.getAsJsonObject().getAsJsonPrimitive("description").getAsString() :
						"Undefined Error.";
				if (postStream.statusCode() == 422) {
					return new EmbedResponse()
							.setTitle("Could not change your display name.")
							.setDescription(errorDescription)
							.setColor(0x5D3E40)
							.markEphemeral();
				}
				return new EmbedResponse()
						.setTitle("Encountered an exception whilst attempting to modify your display name.")
						.setDescription(postStream.statusCode() + ": " + errorDescription + "\nPlease report this to a team member.")
						.setColor(0xFF0000)
						.markEphemeral();
			}

			String title = new BufferedReader(
					new InputStreamReader(postStream.body(), StandardCharsets.UTF_8)
			).lines().collect(Collectors.joining("\n"));

			return new EmbedResponse()
					.setTitle(title)
					.setColor(0xA9FFA7)
					.markEphemeral();
		} catch (IOException | InterruptedException ex) {
			GardenBot.LOG.error("", ex);
			return new EmbedResponse()
					.setTitle("Encountered an exception whilst attempting to modify your display name.")
					.setDescription(ex.getMessage() + "\nPlease report this to a team member.")
					.setColor(0xFF0000)
					.markEphemeral();
		}
	}

	public static Response handleModifyPronouns(SlashCommandInteraction interaction) {
		User user = interaction.event().getUser();

		try {
			HttpResponse<InputStream> userStream = ModGardenAPIClient.get("user/" + user.getId() + "?service=discord", HttpResponse.BodyHandlers.ofInputStream());
			if (userStream.statusCode() == 404) {
				return new MessageResponse()
						.setMessage("You do not have a Mod Garden account.\nPlease create one with **/account create**.")
						.markEphemeral();
			} else if (userStream.statusCode() != 200) {
				JsonElement json = JsonParser.parseReader(new InputStreamReader(userStream.body()));
				String errorDescription = json.isJsonObject() && json.getAsJsonObject().has("description") ?
						json.getAsJsonObject().getAsJsonPrimitive("description").getAsString() :
						"Undefined Error.";
				return new EmbedResponse()
						.setTitle("Encountered an exception whilst attempting to modify your pronouns.")
						.setDescription(userStream.statusCode() + ": " + errorDescription + "\nPlease report this to a team member.")
						.setColor(0xFF0000)
						.markEphemeral();
			}


			OptionMapping value = Objects.requireNonNull(interaction.event().getOption("pronouns"));
			JsonObject inputJson = new JsonObject();
			inputJson.addProperty("discord_id", user.getId());
			inputJson.addProperty("value", value.getAsString());

			HttpResponse<InputStream> postStream = ModGardenAPIClient.post(
					"discord/modify/pronouns",
					HttpRequest.BodyPublishers.ofString(inputJson.toString()),
					HttpResponse.BodyHandlers.ofInputStream(),
					"Authorization", "Basic " + GardenBot.DOTENV.get("OAUTH_SECRET"),
					"Content-Type", "application/json"
			);
			if (postStream.statusCode() < 200 || postStream.statusCode() > 299) {
				JsonElement json = JsonParser.parseReader(new InputStreamReader(postStream.body()));
				String errorDescription = json.isJsonObject() && json.getAsJsonObject().has("description") ?
						json.getAsJsonObject().getAsJsonPrimitive("description").getAsString() :
						"Undefined Error.";
				if (postStream.statusCode() == 422) {
					return new EmbedResponse()
							.setTitle("Could not change your pronouns.")
							.setDescription(errorDescription)
							.setColor(0x5D3E40)
							.markEphemeral();
				}
				return new EmbedResponse()
						.setTitle("Encountered an exception whilst attempting to modify your pronouns.")
						.setDescription(postStream.statusCode() + ": " + errorDescription + "\nPlease report this to a team member.")
						.setColor(0xFF0000)
						.markEphemeral();
			}

			String title = new BufferedReader(
					new InputStreamReader(postStream.body(), StandardCharsets.UTF_8)
			).lines().collect(Collectors.joining("\n"));

			return new EmbedResponse()
					.setTitle(title)
					.setColor(0xA9FFA7)
					.markEphemeral();
		} catch (IOException | InterruptedException ex) {
			GardenBot.LOG.error("", ex);
			return new EmbedResponse()
					.setTitle("Encountered an exception whilst attempting to modify your pronouns.")
					.setDescription(ex.getMessage() + "\nPlease report this to a team member.")
					.setColor(0xFF0000)
					.markEphemeral();
		}
	}


	public static Response removePronouns(SlashCommandInteraction interaction) {
		User user = interaction.event().getUser();

		try {
			HttpResponse<InputStream> userStream = ModGardenAPIClient.get("user/" + user.getId() + "?service=discord", HttpResponse.BodyHandlers.ofInputStream());
			if (userStream.statusCode() == 404) {
				return new MessageResponse()
						.setMessage("You do not have a Mod Garden account.\nPlease create one with **/account create**.")
						.markEphemeral();
			} else if (userStream.statusCode() != 200) {
				JsonElement json = JsonParser.parseReader(new InputStreamReader(userStream.body()));
				String errorDescription = json.isJsonObject() && json.getAsJsonObject().has("description") ?
						json.getAsJsonObject().getAsJsonPrimitive("description").getAsString() :
						"Undefined Error.";
				return new EmbedResponse()
						.setTitle("Encountered an exception whilst attempting to remove your pronouns.")
						.setDescription(userStream.statusCode() + ": " + errorDescription + "\nPlease report this to a team member.")
						.setColor(0xFF0000)
						.markEphemeral();
			}

			JsonObject inputJson = new JsonObject();
			inputJson.addProperty("discord_id", user.getId());

			HttpResponse<InputStream> postStream = ModGardenAPIClient.post(
					"discord/remove/pronouns",
					HttpRequest.BodyPublishers.ofString(inputJson.toString()),
					HttpResponse.BodyHandlers.ofInputStream(),
					"Authorization", "Basic " + GardenBot.DOTENV.get("OAUTH_SECRET"),
					"Content-Type", "application/json"
			);
			if (postStream.statusCode() < 200 || postStream.statusCode() > 299) {
				JsonElement json = JsonParser.parseReader(new InputStreamReader(postStream.body()));
				String errorDescription = json.isJsonObject() && json.getAsJsonObject().has("description") ?
						json.getAsJsonObject().getAsJsonPrimitive("description").getAsString() :
						"Undefined Error.";
				return new EmbedResponse()
						.setTitle("Encountered an exception whilst attempting to remove your pronouns.")
						.setDescription(userStream.statusCode() + ": " + errorDescription + "\nPlease report this to a team member.")
						.setColor(0xFF0000)
						.markEphemeral();
			}

			String title = new BufferedReader(
					new InputStreamReader(postStream.body(), StandardCharsets.UTF_8)
			).lines().collect(Collectors.joining("\n"));

			return new EmbedResponse()
					.setTitle(title)
					.setColor(0xA9FFA7)
					.markEphemeral();
		} catch (IOException | InterruptedException ex) {
			GardenBot.LOG.error("", ex);
			return new EmbedResponse()
					.setTitle("Encountered an exception whilst attempting to remove your pronouns.")
					.setDescription(ex.getMessage() + "\nPlease report this to a team member.")
					.setColor(0xFF0000)
					.markEphemeral();
		}
	}
}
