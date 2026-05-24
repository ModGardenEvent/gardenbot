package net.modgarden.gardenbot.command.team;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.requests.RestAction;
import net.modgarden.gardenbot.GardenBot;
import net.modgarden.gardenbot.GardenBotButtons;
import net.modgarden.gardenbot.command.AutoCompletionGetter;
import net.modgarden.gardenbot.command.SlashCommand;
import net.modgarden.gardenbot.command.SlashCommandOption;
import net.modgarden.gardenbot.database.DatabaseAccess;
import net.modgarden.gardenbot.interaction.SlashCommandInteraction;
import net.modgarden.gardenbot.response.EmbedResponse;
import net.modgarden.gardenbot.response.MessageResponse;
import net.modgarden.gardenbot.response.Response;
import net.modgarden.gardenbot.util.ModGardenAPIClient;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.http.HttpResponse;
import java.util.*;

public class InviteCommand extends SlashCommand {
	private static final int ADMINISTRATOR_PERMISSION_BITS = 0x1;
	private static final int EDIT_PROJECT_PERMISSION_BITS = 0x20;

	public InviteCommand() {
		super(
				"invite",
				"Invite a user to a Mod Garden project's team.",
				new SlashCommandOption(
						OptionType.STRING,
						"project",
						"The project to invite the user to.",
						true
				),
				new SlashCommandOption(
						OptionType.USER,
						"user",
						"The user to invite to the project.",
						true
				),
				new SlashCommandOption(
						OptionType.STRING,
						"role",
						"The name of the user's role. (Defaults to 'Member')",
						false
				)
		);
	}

	@NotNull
	@Override
	public Response respond(SlashCommandInteraction interaction) {
		interaction.event().deferReply(true).queue();
		User user = interaction.event().getUser();

		String project = interaction.event().getOption("project", OptionMapping::getAsString);
		User invitedUser = interaction.event().getOption("user", OptionMapping::getAsUser);
		String role = interaction.event().getOption("role", "Member", OptionMapping::getAsString);

		assert project != null;
		assert invitedUser != null;

		try {
			HttpResponse<InputStream> getUserStream = ModGardenAPIClient.get("v2/users/" + user.getId() + "?by=integration_discord", HttpResponse.BodyHandlers.ofInputStream());
			if (getUserStream.statusCode() != 200) {
				return new MessageResponse("""
						You do not have a Mod Garden account.
						Please create one with **/account create**.""")
						.markEphemeral();
			}
			JsonElement userJson = JsonParser.parseReader(new InputStreamReader(getUserStream.body()));
			ModGardenUser modGardenUser = GardenBot.GSON.fromJson(userJson, ModGardenUser.class);

			HttpResponse<InputStream> getInvitedUserStream = ModGardenAPIClient.get("v2/users/" + invitedUser.getId() + "?by=integration_discord", HttpResponse.BodyHandlers.ofInputStream());
			if (getInvitedUserStream.statusCode() != 200) {
				return new MessageResponse("The user you attempted to invite does not have a Mod Garden account.")
						.markEphemeral();
			}
			JsonElement invitedUserJson = JsonParser.parseReader(new InputStreamReader(getInvitedUserStream.body()));
			ModGardenUser invitedModGardenUser = GardenBot.GSON.fromJson(invitedUserJson, ModGardenUser.class);

			ModGardenProject modGardenProject;
			HttpResponse<InputStream> byIdProjectStream = ModGardenAPIClient.get("v2/projects/" + project, HttpResponse.BodyHandlers.ofInputStream());
			if (byIdProjectStream.statusCode() != 200) {
				return new MessageResponse("Could not find project '" + project + "'.")
						.markEphemeral();
			}
			JsonElement projectJson = JsonParser.parseReader(new InputStreamReader(byIdProjectStream.body()));
			modGardenProject = GardenBot.GSON.fromJson(projectJson, ModGardenProject.class);

			if (!modGardenProject.permissions.containsKey(modGardenUser.id)) {
				return new MessageResponse("You are not a member of project '" + project + "'.")
						.markEphemeral();
			}

			long userPermissions = Long.parseLong(modGardenProject.permissions.get(modGardenUser.id));
			if (!hasPermissions(userPermissions)) {
				return new MessageResponse("You are not allowed to invite users to project '" + project + "'.")
						.markEphemeral();
			}

			if (modGardenProject.permissions.containsKey(invitedModGardenUser.id)) {
				return new MessageResponse("User " + invitedUser.getAsMention() + " is already a member of project '" + project + "'.")
						.markEphemeral();
			}

			DatabaseAccess db = DatabaseAccess.get();
			String inviteCode = db.createTeamInvite(invitedModGardenUser.id, modGardenProject.id, modGardenProject.metadata.name, role);

			EmbedBuilder embedBuilder = new EmbedBuilder()
					.setTitle("You have been invited to project " + modGardenProject.metadata.name + " as a(n) " + role)
					.setDescription("""
						*You were invited by <@%s>*

						You may either Accept or Decline by using the buttons below."""
							.formatted(interaction.event().getUser().getId())
					).setColor(0xA9FFA7);


			PrivateChannel privateChannel = invitedUser.openPrivateChannel().complete();

			if (privateChannel == null) {
				return new EmbedResponse()
						.setTitle("Failed to invite " + invitedUser.getGlobalName() + " to your project.")
						.setDescription("The invited user does not have DMs open within the Mod Garden Discord.")
						.markEphemeral()
						.setColor(0xFF0000);
			}

			privateChannel
					.sendMessageEmbeds(embedBuilder.build())
					.addActionRow(
							Button.of(
									ButtonStyle.SUCCESS,
									GardenBotButtons.ACCEPT_TEAM_INVITE.withArguments(inviteCode),
									"Accept",
									Emoji.fromUnicode("\uD83C\uDF39")
							),
							Button.of(
									ButtonStyle.DANGER,
									GardenBotButtons.DECLINE_TEAM_INVITE.withArguments(inviteCode),
									"Decline",
									Emoji.fromUnicode("\uD83E\uDD40")
							)
					).queue();

			return new EmbedResponse()
					.setTitle("Successfully invited " + invitedUser.getGlobalName() + " to your project.")
					.setDescription("They have received a DM that will let them either accept or deny the invitation.")
					.markEphemeral()
					.setColor(0xA9FFA7);
		} catch (Exception ex) {
			GardenBot.LOG.error("", ex);
			return new EmbedResponse()
					.setTitle("Encountered an exception whilst attempting to invite user to your project.")
					.setDescription(ex.getMessage() + "\nPlease report this to a team member.")
					.markEphemeral()
					.setColor(0xFF0000);
		}
	}

	private boolean hasPermissions(long userPermissions) {
		boolean hasPermissions = (EDIT_PROJECT_PERMISSION_BITS & userPermissions) > 0;
		boolean hasAdministrator = (ADMINISTRATOR_PERMISSION_BITS & userPermissions) != 0;
		return hasAdministrator || hasPermissions;
	}

	@Override
	public List<Command.Choice> getAutoCompleteChoices(String focusedOption,
	                                              User user,
	                                              AutoCompletionGetter autoCompletionGetter) {
		if (focusedOption.equals("user")) {
			return Collections.emptyList();
		}

		return TeamCommand.getProjectAutoCompleteChoices(user, autoCompletionGetter);
	}

	private static class ModGardenUser {
		public String id;
	}

	private static class ModGardenProject {
		public String id;
		public ProjectMetadata metadata;
		public Map<String, String> permissions;
	}

	private static class ProjectMetadata {
		public String name;
	}
}
