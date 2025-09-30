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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.*;
import java.util.stream.Collectors;

public class UpdateHandler {
	public static Response handleUpdate(SlashCommandInteraction interaction) {
		interaction.event().deferReply(true).queue();
		User user = interaction.event().getUser();

		JsonObject inputJson = new JsonObject();
		inputJson.addProperty("discord_id", user.getId());

		String project = interaction.event().getOption("project", OptionMapping::getAsString);
		inputJson.addProperty("slug", project);

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

	// TODO: Clean this up.
	public static List<Command.Choice> getChoices(String focusedOption, User user,
												  AbstractSlashCommand.CompletionFunction completionFunction) {
		if (focusedOption.equals("project")) {
			try {
				var userResult = ModGardenAPIClient.get("user/" + user.getId() + "?service=discord", HttpResponse.BodyHandlers.ofInputStream());
				var eventResult = ModGardenAPIClient.get("events/current/development", HttpResponse.BodyHandlers.ofInputStream());
				if (userResult.statusCode() == 200 && eventResult.statusCode() == 200) {
					try (InputStreamReader userReader = new InputStreamReader(userResult.body());
						 InputStreamReader eventReader = new InputStreamReader(eventResult.body())) {
						ModGardenUser modGardenUser = GardenBot.GSON.fromJson(userReader, ModGardenUser.class);
						ModGardenEvent currentEvent = GardenBot.GSON.fromJson(eventReader, ModGardenEvent.class);
						var submissionsStream = ModGardenAPIClient.get("user/" + modGardenUser.id + "/submissions/" + currentEvent.slug, HttpResponse.BodyHandlers.ofInputStream());
						if (submissionsStream.statusCode() == 200) {
							try (InputStreamReader submissionsReader = new InputStreamReader(submissionsStream.body())) {
								JsonElement submissionsJson = JsonParser.parseReader(submissionsReader);
								if (submissionsJson.isJsonArray()) {
									return submissionsJson.getAsJsonArray().asList().stream().map(submissionJson -> {
										try {
											if (submissionJson.isJsonObject()) {
												ModGardenProject modGardenProject = GardenBot.GSON.fromJson(submissionJson.getAsJsonObject().get("project"), ModGardenProject.class);

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
			} catch (Exception ex) {
				GardenBot.LOG.error("Could not get Discord user's submitted entries to the current event.", ex);
			}
			return Collections.emptyList();
		} else if (focusedOption.equals("version")) {
			String project = completionFunction.getOption("project", OptionMapping::getAsString);

			if (!project.matches("[a-zA-Z0-9!@$()`.+,_\"-]+")) {
				return Collections.emptyList();
			}

			try {
				var eventStream = ModGardenAPIClient.get("events/current/development", HttpResponse.BodyHandlers.ofInputStream());
				var modrinthStream = ModrinthAPIClient.get("v2/project/" + project, HttpResponse.BodyHandlers.ofInputStream());
				if (modrinthStream.statusCode() == 200 && eventStream.statusCode() == 200) {
					try (InputStreamReader modrinthReader = new InputStreamReader(modrinthStream.body());
						 InputStreamReader eventReader = new InputStreamReader(eventStream.body())) {
						ModrinthProject modrinthProject = GardenBot.GSON.fromJson(modrinthReader, ModrinthProject.class);
						ModGardenEvent modGardenEvent = GardenBot.GSON.fromJson(eventReader, ModGardenEvent.class);
						List<ModrinthVersion> modrinthVersions = modrinthProject.versions.parallelStream().map(versionId -> {
							try {
								var versionStream = ModrinthAPIClient.get("v2/version/" + versionId, HttpResponse.BodyHandlers.ofInputStream());
								if (versionStream.statusCode() != 200)
									return null;

								try (InputStreamReader versionReader = new InputStreamReader(versionStream.body())) {
									ModrinthVersion potentialVersion = GardenBot.GSON.fromJson(versionReader, ModrinthVersion.class);

									if (!potentialVersion.gameVersions.contains(modGardenEvent.minecraftVersion))
										return null;

									// Handle natively supported mods for the event's loader.
									if (potentialVersion.loaders.contains(modGardenEvent.loader)) {
										return potentialVersion;
										// Handle Fabric mods loaded via Connector on NeoForge.
									} else if (modGardenEvent.loader.equals("neoforge") && potentialVersion.loaders.contains("fabric")) {
										return potentialVersion;
									}
								}
							} catch (Exception ex) {
								GardenBot.LOG.error("Failed to read Modrinth version.", ex);
							}
							return null;
						}).filter(Objects::nonNull).collect(Collectors.toCollection(ArrayList::new));

						if (modrinthVersions.stream().anyMatch(v -> v.loaders.contains(modGardenEvent.loader))) {
							modrinthVersions.removeIf(v -> !v.loaders.contains(modGardenEvent.loader));
						}

						return modrinthVersions.stream()
								.sorted(Comparator.<ModrinthVersion>comparingLong(value ->
										ZonedDateTime.parse(value.datePublished, DateTimeFormatter.ISO_OFFSET_DATE_TIME).getLong(ChronoField.INSTANT_SECONDS)).reversed())
								.map(modrinthVersion -> new Command.Choice(modrinthVersion.name, modrinthVersion.id))
								.toList();
					}
				}
			} catch (Exception ex) {
				GardenBot.LOG.error("Failed to obtain Modrinth versions for project '{}'.", project, ex);
			}
			return Collections.emptyList();
		}
		return List.of(new Command.Choice("Modrinth", "modrinth"));
	}

	private static class ModGardenUser {
		String id;
	}

	private static class ModGardenProject {
		String slug;
		@SerializedName("modrinth_id")
		String modrinthId;
	}

	private static class ModGardenEvent {
		String slug;

		String loader;
		@SerializedName("minecraft_version")
		String minecraftVersion;
	}

	@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
	private static class ModrinthProject {
		String title;
		List<String> versions;
	}

	@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
	private static class ModrinthVersion {
		String id;
		String name;
		@SerializedName("date_published")
		String datePublished;

		@SerializedName("game_versions")
		List<String> gameVersions;
		List<String> loaders;
	}
}
