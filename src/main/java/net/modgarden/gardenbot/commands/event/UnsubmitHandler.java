package net.modgarden.gardenbot.commands.event;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.modgarden.gardenbot.GardenBot;
import net.modgarden.gardenbot.interaction.SlashCommandInteraction;
import net.modgarden.gardenbot.interaction.command.AbstractSlashCommand;
import net.modgarden.gardenbot.interaction.response.EmbedResponse;
import net.modgarden.gardenbot.interaction.response.Response;
import net.modgarden.gardenbot.util.ModGardenAPIClient;
import net.modgarden.gardenbot.util.ModrinthAPIClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class UnsubmitHandler {
	public static Response handleUnsubmit(SlashCommandInteraction interaction) {
		interaction.event().deferReply(true).queue();
		User user = interaction.event().getUser();

		JsonObject inputJson = new JsonObject();
		inputJson.addProperty("discord_id", user.getId());

		String project = interaction.event().getOption("project", OptionMapping::getAsString);
		inputJson.addProperty("slug", project);
		try {
			HttpResponse<InputStream> stream = ModGardenAPIClient.post(
					"discord/submission/delete",
					HttpRequest.BodyPublishers.ofString(inputJson.toString()),
					HttpResponse.BodyHandlers.ofInputStream(),
					"Authorization", "Basic " + GardenBot.DOTENV.get("OAUTH_SECRET"),
					"Content-Type", "application/json"
			);
			JsonElement json = JsonParser.parseReader(new InputStreamReader(stream.body()));
			if (stream.statusCode() == 401 || stream.statusCode() == 422) {
				String errorDescription = json.isJsonObject() && json.getAsJsonObject().has("description") ?
						json.getAsJsonObject().getAsJsonPrimitive("description").getAsString() :
						"Undefined Error.";
				return new EmbedResponse()
						.setTitle("Could not unsubmit your Modrinth project from Mod Garden.")
						.setDescription(errorDescription)
						.setColor(0x5D3E40);
			} else if (stream.statusCode() < 200 || stream.statusCode() > 299) {
				String errorDescription = json.isJsonObject() && json.getAsJsonObject().has("description") ?
						json.getAsJsonObject().getAsJsonPrimitive("description").getAsString() :
						"Undefined Error.";
				return new EmbedResponse()
						.setTitle("Encountered an exception whilst attempting to unsubmit your Modrinth project from Mod Garden.")
						.setDescription(stream.statusCode() + ": " + errorDescription + "\nPlease report this to a team member.")
						.setColor(0xFF0000);
			}

			String resultDescription = json.isJsonObject() && json.getAsJsonObject().has("description") ?
					json.getAsJsonObject().getAsJsonPrimitive("description").getAsString() :
					"Undefined Result.";
			return new EmbedResponse()
					.setTitle(resultDescription)
					.setColor(0xA9FFA7);
		} catch (IOException | InterruptedException ex) {
			GardenBot.LOG.error("", ex);
			return new EmbedResponse()
					.setTitle("Encountered an exception whilst attempting to unsubmit your Modrinth project from Mod Garden.")
					.setDescription(ex.getMessage() + "\nPlease report this to a team member.")
					.setColor(0xFF0000);
		}
	}

	public static List<Command.Choice> getChoices(String focusedOption, User user,
												  AbstractSlashCommand.CompletionFunction completionFunction)  {
		List<Command.Choice> choices = new ArrayList<>();
		try {
			var userResult = ModGardenAPIClient.get("user/" + user.getId() + "?service=discord", HttpResponse.BodyHandlers.ofInputStream());
			var eventResult = ModGardenAPIClient.get("events/current/development", HttpResponse.BodyHandlers.ofInputStream());
			if (userResult.statusCode() == 200 && eventResult.statusCode() == 200) {
				try (InputStreamReader userReader = new InputStreamReader(userResult.body());
					 InputStreamReader eventReader = new InputStreamReader(eventResult.body())) {
					ModGardenUser modGardenUser = GardenBot.GSON.fromJson(userReader, ModGardenUser.class);
					ModGardenEvent modGardenEvent = GardenBot.GSON.fromJson(eventReader, ModGardenEvent.class);
					var submissionsStream = ModGardenAPIClient.get("user/" + modGardenUser.id + "/submissions/" + modGardenEvent.slug, HttpResponse.BodyHandlers.ofInputStream());
					if (submissionsStream.statusCode() == 200) {
						try (InputStreamReader submissionsReader = new InputStreamReader(submissionsStream.body())) {
							JsonElement submissionsJson = JsonParser.parseReader(submissionsReader);
							if (submissionsJson.isJsonArray()) {
								return submissionsJson.getAsJsonArray().asList().stream().map(submissionJson -> {
									try {
										if (submissionJson.isJsonObject()) {
											var modGardenStream = ModGardenAPIClient.get("project/" + submissionJson.getAsJsonObject().get("project_id").getAsString(), HttpResponse.BodyHandlers.ofInputStream());
											if (modGardenStream.statusCode() == 200) {
												try (InputStreamReader modGardenReader = new InputStreamReader(modGardenStream.body())) {
													ModGardenProject modGardenProject = GardenBot.GSON.fromJson(modGardenReader, ModGardenProject.class);

													String slug = modGardenProject.slug;
													String title = slug;

													var modrinthStream = ModrinthAPIClient.get("v2/project/" + modGardenProject.modrinthId, HttpResponse.BodyHandlers.ofInputStream());
													if (modrinthStream.statusCode() == 200) {
														try (InputStreamReader modrinthReader = new InputStreamReader(modrinthStream.body())) {
															ModrinthProject modrinthProject = GardenBot.GSON.fromJson(modrinthReader, ModrinthProject.class);
															title = modrinthProject.title;
														}
													}
													return new Command.Choice(title, slug);
												}
											}
										}
									} catch (Exception ex) {
										GardenBot.LOG.error("Failed to read Modrinth version.", ex);
									}
									return null;
								}).filter(Objects::nonNull).toList();
							}
						}
					}
				}
			}
		} catch (IOException | InterruptedException ex) {
			GardenBot.LOG.error("Could not get Discord user's submitted entries to the current event.", ex);
		}
		return choices;
	}

	private static class ModGardenUser {
		@SerializedName("id")
		String id;
	}

	private static class ModGardenProject {
		String slug;
		@SerializedName("modrinth_id")
		String modrinthId;
	}

	private static class ModrinthProject {
		String title;
	}

	private static class ModGardenEvent {
		String slug;
	}
}
