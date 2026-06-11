package net.modgarden.gardenbot.command.team;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
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
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static net.modgarden.gardenbot.command.team.TeamCommand.*;
import static net.modgarden.gardenbot.util.MiscUtil.aOrAn;

public class TeamInviteCommand extends SlashCommand {
	public TeamInviteCommand() {
		super(
				"invite",
				"Invite a user to a Mod Garden project's team.",
				new SlashCommandOption(
						OptionType.STRING,
						"project",
						"The project to invite the user to.",
						true,
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
			TeamCommand.ModGardenUser modGardenUser = getModGardenUser(user);
			if (modGardenUser == null) {
				return new MessageResponse("""
						You do not have a Mod Garden account.
						Please create one with **/account create**."""
				).markEphemeral();
			}

			TeamCommand.ModGardenUser invitedModGardenUser = getModGardenUser(invitedUser);
			if (invitedModGardenUser == null) {
				return new MessageResponse("The user you attempted to invite does not have a Mod Garden account.")
						.markEphemeral();
			}

			TeamCommand.ModGardenProject modGardenProject = getProject(project);
			if (modGardenProject == null) {
				return new MessageResponse("Could not find project '" + project + "'.")
						.markEphemeral();
			}

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

			if (db.updateTeamInvite(invitedModGardenUser.id, modGardenProject.id, role)) {
				return new EmbedResponse()
						.setTitle("Updated the expiry date invited " + invitedUser.getGlobalName() + " to your project.")
						.setDescription("They should use the existing DM .")
						.markEphemeral()
						.setColor(0xA9FFA7);
			}

			String inviteCode = db.createTeamInvite(invitedModGardenUser.id, modGardenProject.id, role);

			EmbedBuilder embedBuilder = new EmbedBuilder()
					.setTitle("You have been invited to project %s as %s %s"
							.formatted(modGardenProject.metadata.name, aOrAn(role), role)
					).setDescription("""
						*You were invited by %s*

						You may either Accept or Decline by using the buttons below."""
							.formatted(interaction.event().getUser().getAsMention())
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
					.setTitle("Encountered an exception!")
					.setDescription(ex.getMessage() + "\nPlease report this to a team member.")
					.markEphemeral()
					.setColor(0xFF0000);
		}
	}

	@Override
	public List<Command.Choice> getAutoCompleteChoices(String focusedOption,
													   User user,
													   AutoCompletionGetter autoCompletionGetter) {
		if (focusedOption.equals("user")) {
			return Collections.emptyList();
		}

		return getEditableProjectAutoCompleteChoices(user);
	}
}
