package net.modgarden.gardenbot.commands.event;

import com.google.gson.*;
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
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.stream.Stream;

public class UpdateHandler {
	public static Response handleUpdate(SlashCommandInteraction interaction) {
		interaction.event().deferReply(true).queue();
		User user = interaction.event().getUser();

		JsonObject inputJson = new JsonObject();
		inputJson.addProperty("discord_id", user.getId());

		String slug = interaction.event().getOption("slug", OptionMapping::getAsString);
		inputJson.addProperty("slug", slug);

		String version = interaction.event().getOption("version", OptionMapping::getAsString);
		if (version != null) {
			inputJson.addProperty("version", version);
		}

		String source = interaction.event().getOption("source", OptionMapping::getAsString);
		if (source == null || !"modrinth".equals(source.toLowerCase(Locale.ROOT))) {
			return new EmbedResponse()
					.setTitle("Could not update your project within Mod Garden's database.")
					.setDescription("Invalid mod source.")
					.setColor(0x5D3E40);
		}

		try {
			HttpResponse<InputStream> stream = ModGardenAPIClient.post(
					"discord/submission/modify/version/" + source.toLowerCase(Locale.ROOT),
					HttpRequest.BodyPublishers.ofString(inputJson.toString()),
					HttpResponse.BodyHandlers.ofInputStream(),
					"Content-Type", "application/json"
			);
			JsonElement json = JsonParser.parseReader(new InputStreamReader(stream.body()));
			if (stream.statusCode() == 401 || stream.statusCode() == 422) {
				String errorDescription = json.isJsonObject() && json.getAsJsonObject().has("description") ?
						json.getAsJsonObject().getAsJsonPrimitive("description").getAsString() :
						"Undefined Error.";
				return new EmbedResponse()
						.setTitle("Could not update your project within Mod Garden's database.")
						.setDescription(errorDescription)
						.setColor(0x5D3E40);
			} else if (stream.statusCode() < 200 || stream.statusCode() > 299) {
				String errorDescription = json.isJsonObject() && json.getAsJsonObject().has("description") ?
						json.getAsJsonObject().getAsJsonPrimitive("description").getAsString() :
						"Undefined Error.";
				return new EmbedResponse()
						.setTitle("Encountered an exception whilst attempting to update your project within Mod Garden's database.")
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
					.setTitle("Encountered an exception whilst attempting to update your project within Mod Garden's database.")
					.setDescription(ex.getMessage() + "\nPlease report this to a team member.")
					.setColor(0xFF0000);
		}
	}

	public static List<Command.Choice> getChoices(String focusedOption, User user,
												  AbstractSlashCommand.CompletionFunction completionFunction) {
		if (focusedOption.equals("slug")) {
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
								var projectStream = ModGardenAPIClient.get("project/" + submissionJson.getAsJsonObject().get("project_id").getAsString(), HttpResponse.BodyHandlers.ofInputStream());
								if (projectStream.statusCode() == 200) {
									JsonElement modGardenProject = JsonParser.parseReader(new InputStreamReader(projectStream.body()));
									var modrinthStream = ModrinthAPIClient.get("v2/project/" + modGardenProject.getAsJsonObject().getAsJsonPrimitive("modrinth_id").getAsString(), HttpResponse.BodyHandlers.ofInputStream());
									String slug = modGardenProject.getAsJsonObject().getAsJsonPrimitive("slug").getAsString();
									String title = slug;
									if (modrinthStream.statusCode() == 200) {
										JsonElement modrinthProject = JsonParser.parseReader(new InputStreamReader(modrinthStream.body()));
										title = modrinthProject.getAsJsonObject().getAsJsonPrimitive("title").getAsString();
									}
									choices.add(new Command.Choice(title, slug));
								}
							}
						}
					}
				}
			} catch (Exception ex) {
				GardenBot.LOG.error("Could not get Discord user's submitted entries to the current event.", ex);
			}
			return choices;
		} else if (focusedOption.equals("version")) {
			return Collections.emptyList();
		}
		return List.of(new Command.Choice("Modrinth", "modrinth"));
	}

	private static class ModGardenUser {
		String id;
	}

	private static class CurrentEvent {
		String slug;

		String loader;
		@SerializedName("minecraft_version")
		String minecraftVersion;
	}

	@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
	private static class ModrinthVersion {
		String name;
		@Nullable
		String id;

		@SerializedName("game_versions")
		List<String> gameVersions;
		List<String> loaders;
	}
}
