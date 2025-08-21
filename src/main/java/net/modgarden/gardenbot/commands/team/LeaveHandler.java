package net.modgarden.gardenbot.commands.team;

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
import net.modgarden.gardenbot.util.Pair;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.stream.Collectors;

public class LeaveHandler {
	public static Response handleLeave(SlashCommandInteraction interaction) {
		interaction.event().deferReply(true).queue();

		JsonObject inputJson = new JsonObject();
		String projectSlug = interaction.event().getOption("project", OptionMapping::getAsString);
		if (projectSlug != null && !projectSlug.matches(GardenBot.SAFE_URL_REGEX)) {
			return new EmbedResponse()
					.setTitle("Could not leave project.")
					.setDescription("Invalid project slug.")
					.markEphemeral()
					.setColor(0x5D3E40);
		}

		ModGardenProject project;
		try {
			var projectResult = ModGardenAPIClient.get("project/" + projectSlug, HttpResponse.BodyHandlers.ofInputStream());
			if (projectResult.statusCode() != 200) {
				return new EmbedResponse()
						.setTitle("Could not leave project.")
						.setDescription("Project '" + projectSlug + "' has not been submitted to Mod Garden.")
						.markEphemeral()
						.setColor(0x5D3E40);
			}
			try (InputStreamReader projectStream = new InputStreamReader(projectResult.body())) {
				project = GardenBot.GSON.fromJson(JsonParser.parseReader(projectStream), ModGardenProject.class);
			}
		} catch (IOException | InterruptedException ex) {
			GardenBot.LOG.error("", ex);
			return new EmbedResponse()
					.setTitle("Encountered an exception whilst attempting to leave project.")
					.setDescription(ex.getMessage() + "\nPlease report this to a team member.")
					.markEphemeral()
					.setColor(0xFF0000);
		}
		inputJson.addProperty("project_id", project.id);

		String modrinthTitle;
		try {
			var modrinthResult = ModrinthAPIClient.get("v2/project/" + project.modrinthId, HttpResponse.BodyHandlers.ofInputStream());
			if (modrinthResult.statusCode() != 200) {
				modrinthTitle = projectSlug;
			} else {
				try (InputStreamReader projectStream = new InputStreamReader(modrinthResult.body())) {
					modrinthTitle = GardenBot.GSON.fromJson(JsonParser.parseReader(projectStream), ModrinthProject.class).title;
				}
			}
		} catch (IOException | InterruptedException ex) {
			GardenBot.LOG.error("", ex);
			return new EmbedResponse()
					.setTitle("Encountered an exception whilst attempting to leave project.")
					.setDescription(ex.getMessage() + "\nPlease report this to a team member.")
					.markEphemeral()
					.setColor(0xFF0000);
		}

		ModGardenUser user;
		try {
			var userResult = ModGardenAPIClient.get("user/" + interaction.event().getUser().getId() + "?service=discord", HttpResponse.BodyHandlers.ofInputStream());
			if (userResult.statusCode() != 200) {
				return new EmbedResponse()
						.setTitle("Could not leave project.")
						.setDescription("You are not part of the specified project.")
						.markEphemeral()
						.setColor(0x5D3E40);
			}
			try (InputStreamReader userStream = new InputStreamReader(userResult.body())) {
				user = GardenBot.GSON.fromJson(JsonParser.parseReader(userStream), ModGardenUser.class);
				if (project.attributedTo.contains(user.id) || project.authors.size() == 1) {
					return new EmbedResponse()
							.setTitle("Could not leave project.")
							.setDescription("You are the owner or are the only member of the specified project.")
							.markEphemeral()
							.setColor(0x5D3E40);
				}
				if (!project.authors.contains(user.id)) {
					return new EmbedResponse()
							.setTitle("Could not leave project.")
							.setDescription("You are not part of the specified project.")
							.markEphemeral()
							.setColor(0x5D3E40);
				}
			}
		} catch (IOException | InterruptedException ex) {
			GardenBot.LOG.error("", ex);
			return new EmbedResponse()
					.setTitle("Encountered an exception whilst attempting to leave project.")
					.setDescription(ex.getMessage() + "\nPlease report this to a team member.")
					.markEphemeral()
					.setColor(0xFF0000);
		}
		inputJson.addProperty("user_id", user.id);

		try {
			ModGardenAPIClient.post(
					"discord/project/user/remove",
					HttpRequest.BodyPublishers.ofString(inputJson.toString()),
					HttpResponse.BodyHandlers.discarding(),
					"Content-Type", "application/json");
		} catch (IOException | InterruptedException ex) {
			GardenBot.LOG.error("", ex);
			return new EmbedResponse()
					.setTitle("Encountered an exception whilst attempting to leave project.")
					.setDescription(ex.getMessage() + "\nPlease report this to a team member.")
					.markEphemeral()
					.setColor(0xFF0000);
		}

		return new EmbedResponse()
				.setTitle("Successfully left project '" + modrinthTitle + "'.")
				.markEphemeral()
				.setColor(0xA9FFA7);
	}


	public static List<Command.Choice> getChoices(String focusedOption, User user,
												  AbstractSlashCommand.CompletionFunction completionFunction) {
		ModGardenUser modGardenUser;
		try {
			var userResult = ModGardenAPIClient.get("user/" + user.getId() + "?service=discord", HttpResponse.BodyHandlers.ofInputStream());
			if (userResult.statusCode() != 200) {
				return Collections.emptyList();
			}
			try (InputStreamReader userReader = new InputStreamReader(userResult.body())) {
				modGardenUser = GardenBot.GSON.fromJson(userReader, ModGardenUser.class);
			}
		} catch (IOException | InterruptedException ex) {
			GardenBot.LOG.error("Could not get Discord user's Mod Garden account.", ex);
			return Collections.emptyList();
		}

		// TODO: Clean this up for backend V2 (we will not have to actively lookup Modrinth).
		List<ModGardenProject> modGardenProjects = new ArrayList<>();
		try {
			var projectsResult = ModGardenAPIClient.get("user/" + modGardenUser.id + "/projects", HttpResponse.BodyHandlers.ofInputStream());
			if (projectsResult.statusCode() != 200) {
				return Collections.emptyList();
			}
			try (InputStreamReader projectReader = new InputStreamReader(projectsResult.body())) {
				JsonElement element = JsonParser.parseReader(projectReader);
				if (!element.isJsonArray()) {
					return Collections.emptyList();
				}
				modGardenProjects.addAll(element.getAsJsonArray().asList()
						.parallelStream()
						.map(jsonElement -> {
							ModGardenProject project = GardenBot.GSON.fromJson(jsonElement, ModGardenProject.class);
							if (project.attributedTo.equals(modGardenUser.id) || project.authors.size() == 1) {
								return null;
							}
							return project;
						})
						.filter(Objects::nonNull)
						.toList()
				);
			}
		} catch (IOException | InterruptedException ex) {
			GardenBot.LOG.error("Could not get Discord user's Mod Garden projects.", ex);
		}

		Map<String, ModrinthProject> modrinthIdToModrinthProject = new HashMap<>();
		try {
			var modrinthResult = ModrinthAPIClient.get("v2/projects?ids=[" + modGardenProjects.stream()
					.map(project -> "%22" + project.modrinthId + "%22")
					.collect(Collectors.joining(",")) + "]", HttpResponse.BodyHandlers.ofInputStream());
			if (modrinthResult.statusCode() != 200) {
				return Collections.emptyList();
			}
			try (InputStreamReader projectReader = new InputStreamReader(modrinthResult.body())) {
				JsonElement element = JsonParser.parseReader(projectReader);
				if (!element.isJsonArray()) {
					return Collections.emptyList();
				}
				modrinthIdToModrinthProject.putAll(element.getAsJsonArray().asList()
						.parallelStream()
						.map(jsonElement -> {
							ModrinthProject project = GardenBot.GSON.fromJson(jsonElement, ModrinthProject.class);
							return new Pair<>(project.id, project);
						})
						.collect(Collectors.toMap(
								Pair::first,
								Pair::second,
								(projectA, projectB) -> projectB)
						)
				);
			}
		} catch (IOException | InterruptedException ex) {
			GardenBot.LOG.error("Could not get Discord user's Mod Garden projects.", ex);
		}

		Map<String, String> titleToProject = modGardenProjects
				.parallelStream()
				.map(modGardenProject -> new Pair<>(modrinthIdToModrinthProject.get(modGardenProject.modrinthId).title, modGardenProject.slug))
				.collect(Collectors.toMap(Pair::first, Pair::second, (projectA, projectB) -> projectB));

		return titleToProject.entrySet()
				.parallelStream()
				.map(stringStringEntry -> new Command.Choice(stringStringEntry.getKey(), stringStringEntry.getValue()))
				.toList();
	}

	private static class ModGardenUser {
		String id;
	}

	@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
	private static class ModGardenProject {
		String id;
		String slug;
		@SerializedName("modrinth_id")
		String modrinthId;

		@SerializedName("attributed_to")
		String attributedTo;

		List<String> authors;
	}

	@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
	private static class ModrinthProject {
		String id;
		String title;
	}
}
