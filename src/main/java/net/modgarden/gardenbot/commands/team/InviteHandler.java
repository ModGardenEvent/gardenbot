package net.modgarden.gardenbot.commands.team;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.modgarden.gardenbot.GardenBot;
import net.modgarden.gardenbot.interaction.SlashCommandInteraction;
import net.modgarden.gardenbot.interaction.command.AbstractSlashCommand;
import net.modgarden.gardenbot.interaction.response.EmbedResponse;
import net.modgarden.gardenbot.interaction.response.Response;
import net.modgarden.gardenbot.util.ModGardenAPIClient;
import net.modgarden.gardenbot.util.ModrinthAPIClient;
import net.modgarden.gardenbot.util.Pair;

import java.io.*;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class InviteHandler {
	public static Response handleInvite(SlashCommandInteraction interaction) {
		interaction.event().deferReply(true).queue();

		JsonObject inputJson = new JsonObject();
		String projectSlug = interaction.event().getOption("project", OptionMapping::getAsString);
		if (projectSlug != null && !projectSlug.matches(GardenBot.SAFE_URL_REGEX)) {
			return new EmbedResponse()
					.setTitle("Could not invite user to your project.")
					.setDescription("Unsafe URL.")
					.markEphemeral()
					.setColor(0x5D3E40);
		}

		ModGardenProject project;
		try {
			var projectResult = ModGardenAPIClient.get("project/" + projectSlug, HttpResponse.BodyHandlers.ofInputStream());
			if (projectResult.statusCode() != 200) {
				return new EmbedResponse()
						.setTitle("Could not invite user to your project.")
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
					.setTitle("Encountered an exception whilst attempting to invite user to your project.")
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
					.setTitle("Encountered an exception whilst attempting to invite user to your project.")
					.setDescription(ex.getMessage() + "\nPlease report this to a team member.")
					.markEphemeral()
					.setColor(0xFF0000);
		}

		try {
			var userResult = ModGardenAPIClient.get("user/" + interaction.event().getUser().getId() + "?service=discord", HttpResponse.BodyHandlers.ofInputStream());
			if (userResult.statusCode() != 200) {
				return new EmbedResponse()
						.setTitle("Could not invite user to your project.")
						.setDescription("You do not have a Mod Garden account.\nPlease create one with **/account create**.")
						.markEphemeral()
						.setColor(0x5D3E40);
			}
			try (InputStreamReader userStream = new InputStreamReader(userResult.body())) {
				ModGardenUser user = GardenBot.GSON.fromJson(JsonParser.parseReader(userStream), ModGardenUser.class);
				if (!project.authors.contains(user.id)) {
					return new EmbedResponse()
							.setTitle("Could not invite user to your project.")
							.setDescription("You do not have the permissions to invite users to the specified project.")
							.markEphemeral()
							.setColor(0x5D3E40);
				}
			}
		} catch (IOException | InterruptedException ex) {
			GardenBot.LOG.error("", ex);
			return new EmbedResponse()
					.setTitle("Encountered an exception whilst attempting to invite user to your project.")
					.setDescription(ex.getMessage() + "\nPlease report this to a team member.")
					.markEphemeral()
					.setColor(0xFF0000);
		}

		User invitedDiscordUser = interaction.event().getOption("user", OptionMapping::getAsUser);
		if (invitedDiscordUser == null) {
			return new EmbedResponse()
					.setTitle("Could not invite user to your project.")
					.setDescription("No user specified.")
					.markEphemeral()
					.setColor(0x5D3E40);
		}
		if (invitedDiscordUser.isBot() || invitedDiscordUser.isSystem()) {
			return new EmbedResponse()
					.setTitle("Could not invite user to your project.")
					.setDescription("You may not invite bots to your Mod Garden project.")
					.markEphemeral()
					.setColor(0x5D3E40);
		}

		ModGardenUser invited;
		try {
			var userResult = ModGardenAPIClient.get("user/" + invitedDiscordUser.getId() + "?service=discord", HttpResponse.BodyHandlers.ofInputStream());
			if (userResult.statusCode() != 200) {
				return new EmbedResponse()
						.setTitle("Could not invite user to your project.")
							.setDescription("The specified user does not have a Mod Garden account.")
						.markEphemeral()
						.setColor(0x5D3E40);
			}
			try (InputStreamReader userStream = new InputStreamReader(userResult.body())) {
				invited = GardenBot.GSON.fromJson(JsonParser.parseReader(userStream), ModGardenUser.class);
			}
		} catch (IOException | InterruptedException ex) {
			GardenBot.LOG.error("", ex);
			return new EmbedResponse()
					.setTitle("Encountered an exception whilst attempting to invite user to your project.")
					.setDescription(ex.getMessage() + "\nPlease report this to a team member.")
					.markEphemeral()
					.setColor(0xFF0000);
		}
		inputJson.addProperty("user_id", invited.id);

		String role = interaction.event().getOption("role", OptionMapping::getAsString);
		if (role == null || !role.equals("author") && !role.equals("builder")) {
			return new EmbedResponse()
					.setTitle("Could not invite user to your project.")
					.setDescription("Invalid role.")
					.markEphemeral()
					.setColor(0x5D3E40);
		}
		inputJson.addProperty("role", role);

		String inviteCode;
		try {
			var inviteCodeResult = ModGardenAPIClient.post(
					"discord/project/user/invite",
					HttpRequest.BodyPublishers.ofString(inputJson.toString()),
					HttpResponse.BodyHandlers.ofInputStream(),
					"Content-Type", "application/json");
			try (InputStream inviteCodeStream = inviteCodeResult.body();
				 InputStreamReader inviteCodeReader = new InputStreamReader(inviteCodeResult.body())) {
				if (inviteCodeResult.statusCode() == 200) {
					String message = new String(inviteCodeStream.readAllBytes(), StandardCharsets.UTF_8);
					return new EmbedResponse()
							.setTitle("Could not invite user to your project.")
							.setDescription(message)
							.markEphemeral()
							.setColor(0x5D3E40);
				} else if (inviteCodeResult.statusCode() != 201) {
					JsonElement json = JsonParser.parseReader(inviteCodeReader);
					String errorMessage = json.isJsonObject() && json.getAsJsonObject().has("description") ?
							json.getAsJsonObject().getAsJsonPrimitive("description").getAsString() :
							"Undefined Error.";
					return new EmbedResponse()
							.setTitle("Could not invite user to your project.")
							.setDescription(errorMessage)
							.markEphemeral()
							.setColor(0x5D3E40);
				}
				String potentialInviteCode = new String(inviteCodeStream.readAllBytes(), StandardCharsets.UTF_8);
				// Handle invite extensions separately. Don't know if there's a better code for this, but yeah...
				if (potentialInviteCode.length() > 6) {
					return new EmbedResponse()
							.setTitle(potentialInviteCode)
							.markEphemeral()
							.setColor(0xA9FFA7);
				}
				inviteCode = potentialInviteCode;
			}
		} catch (IOException | InterruptedException ex) {
			GardenBot.LOG.error("", ex);
			return new EmbedResponse()
					.setTitle("Encountered an exception whilst attempting to invite user to your project.")
					.setDescription(ex.getMessage() + "\nPlease report this to a team member.")
					.markEphemeral()
					.setColor(0xFF0000);
		}

		EmbedBuilder embedBuilder = new EmbedBuilder()
				.setTitle("You have been invited to project " + modrinthTitle + " as a(n) " + role.substring(0, 1).toUpperCase() + role.substring(1) + "!")
				.setDescription("""
				*You were invited by <@%s>*

				You may either Accept or Decline by using the buttons below.
				""".formatted(interaction.event().getUser().getId()))
				.setColor(0xA9FFA7);

		invitedDiscordUser.openPrivateChannel()
				.onSuccess(privateChannel -> privateChannel
						.sendMessageEmbeds(embedBuilder.build())
						.addActionRow(
								Button.of(
										ButtonStyle.SUCCESS,
										"acceptInvite?" + inviteCode,
										"Accept",
										Emoji.fromUnicode("\uD83C\uDF39")
								),
								Button.of(
										ButtonStyle.DANGER,
										"declineInvite?" + inviteCode,
										"Decline",
										Emoji.fromUnicode("\uD83E\uDD40")
								)
						)
						.queue())
				.queue();

		return new EmbedResponse()
				.setTitle("Successfully invited " + invitedDiscordUser.getEffectiveName() + " to your project.")
				.setDescription("They will have received a DM which will let them either accept or deny the invitation.")
				.markEphemeral()
				.setColor(0xA9FFA7);
	}

	public static List<Command.Choice> getChoices(String focusedOption, User user,
												  AbstractSlashCommand.CompletionFunction completionFunction) {

		if (focusedOption.equals("user")) {
			return Collections.emptyList();
		} else if (focusedOption.equals("role")) {
			return List.of(new Command.Choice("Author", "author"), new Command.Choice("Builder", "builder"));
		}
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
							if (!project.authors.contains(modGardenUser.id)) {
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

		List<String> authors;
	}

	@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
	private static class ModrinthProject {
		String id;
		String title;
	}
}
