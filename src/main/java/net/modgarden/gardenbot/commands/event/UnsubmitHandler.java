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

public class UnsubmitHandler {
	public static Response handleUnsubmit(SlashCommandInteraction interaction) {
		interaction.event().deferReply(true).queue();
		User user = interaction.event().getUser();

		JsonObject inputJson = new JsonObject();
		inputJson.addProperty("discord_id", user.getId());

		String slug = interaction.event().getOption("slug", OptionMapping::getAsString);
		inputJson.addProperty("slug", slug);

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

	public static List<Command.Choice> getChoices(String focusedOption, User user)  {
		List<Command.Choice> choices = new ArrayList<>();
		try {
			var userResult = ModGardenAPIClient.get("user/" + user.getId() + "?service=discord", HttpResponse.BodyHandlers.ofInputStream());
			var eventResult = ModGardenAPIClient.get("events/current/development", HttpResponse.BodyHandlers.ofInputStream());
			if (userResult.statusCode() == 200 && eventResult.statusCode() == 200) {
				ModGardenUser modGardenUser = GardenBot.GSON.fromJson(new InputStreamReader(userResult.body()), ModGardenUser.class);
				CurrentEvent currentEvent = GardenBot.GSON.fromJson(new InputStreamReader(eventResult.body()), CurrentEvent.class);
				var submissionsEventResult = ModGardenAPIClient.get("user/" + modGardenUser.id + "/submissions/" + currentEvent.slug, HttpResponse.BodyHandlers.ofInputStream());
				if (submissionsEventResult.statusCode() == 200) {
					InputStreamReader activeEventsReader = new InputStreamReader(submissionsEventResult.body());
					JsonElement submissionsJson = JsonParser.parseReader(activeEventsReader);
					if (submissionsJson.isJsonArray()) {
						for (JsonElement submissionJson : submissionsJson.getAsJsonArray()) {
							if (!submissionJson.isJsonObject())
								continue;
							var projectResult = ModGardenAPIClient.get("project/" + submissionJson.getAsJsonObject().get("project_id").getAsString(), HttpResponse.BodyHandlers.ofInputStream());
							if (projectResult.statusCode() == 200) {
								ModGardenProject modGardenProject = GardenBot.GSON.fromJson(new InputStreamReader(projectResult.body()), ModGardenProject.class);
								if (!modGardenProject.attributedTo.equals(modGardenUser.id))
									continue;
								var modrinthStream = ModrinthAPIClient.get("v2/project/" + modGardenProject.modrinthId, HttpResponse.BodyHandlers.ofInputStream());
								String title = modGardenProject.slug;
								if (modrinthStream.statusCode() == 200) {
									ModrinthProject modrinthProject = GardenBot.GSON.fromJson(new InputStreamReader(modrinthStream.body()), ModrinthProject.class);
									title = modrinthProject.title;
								}
								choices.add(new Command.Choice(title, modGardenProject.slug));
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

	private static class CurrentEvent {
		String slug;
	}

	private static class ModGardenProject {
		@SerializedName("attributed_to")
		String attributedTo;

		String slug;

		@SerializedName("modrinth_id")
		String modrinthId;
	}

	private static class ModrinthProject {
		@SerializedName("title")
		String title;
	}
}
