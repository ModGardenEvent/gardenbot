package net.modgarden.gardenbot.command.team;

import static net.modgarden.gardenbot.command.team.TeamCommandGroup.getEditableProjectAutoCompleteChoices;
import static net.modgarden.gardenbot.command.team.TeamCommandGroup.hasPermissions;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.modgarden.gardenbot.GardenBot;
import net.modgarden.gardenbot.client.ModGarden;
import net.modgarden.gardenbot.client.exception.HypertextException;
import net.modgarden.gardenbot.client.mod_garden.project.ModGardenProject;
import net.modgarden.gardenbot.client.mod_garden.project.patch.ProjectTeamPatch;
import net.modgarden.gardenbot.client.mod_garden.user.ModGardenUser;
import net.modgarden.gardenbot.command.AutoCompletionGetter;
import net.modgarden.gardenbot.command.SlashCommand;
import net.modgarden.gardenbot.command.SlashCommandOption;
import net.modgarden.gardenbot.command.admin.role.AdminRoleCreateCommand;
import net.modgarden.gardenbot.interaction.SlashCommandInteraction;
import net.modgarden.gardenbot.response.EmbedResponse;
import net.modgarden.gardenbot.response.MessageResponse;
import net.modgarden.gardenbot.response.Response;
import net.modgarden.gardenbot.util.permission.Permission;
import net.modgarden.gardenbot.util.permission.PermissionScope;
import net.modgarden.gardenbot.util.permission.Permissions;
import org.jetbrains.annotations.NotNull;

public class TeamModifyCommand extends SlashCommand {
	public TeamModifyCommand() {
		super(
				"modify",
				"Modify a user on a Mod Garden project's team.",
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
						"The team member to modify.",
						true
				),
				new SlashCommandOption(
						OptionType.STRING,
						"role",
						"The name of the user's role. (Defaults to 'Member')",
						false
				),
				new SlashCommandOption(
						OptionType.STRING,
						"permissions",
						"The user's permissions. (Defaults to 'None')",
						false,
						true
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
		String role = interaction.event().getOption("role", null, OptionMapping::getAsString);
		BigInteger permissions = AdminRoleCreateCommand.parsePermissions(interaction.event().getOption("permissions", null, OptionMapping::getAsString));

		assert project != null;
		assert invitedUser != null;

		try {
			ModGardenUser modGardenUser = ModGarden.getUserByDiscordUser(user);
			if (modGardenUser == null) {
				return new MessageResponse("""
						You do not have a Mod Garden account.
						Please create one with **/account create**."""
				).markEphemeral();
			}

			ModGardenUser invitedModGardenUser = ModGarden.getUserByDiscordUser(invitedUser);
			if (invitedModGardenUser == null) {
				return new MessageResponse("The user you attempted to invite does not have a Mod Garden account.")
						.markEphemeral();
			}

			ModGardenProject modGardenProject = ModGarden.getProject(project);
			if (modGardenProject == null) {
				return new MessageResponse("Could not find project '" + project + "'.")
						.markEphemeral();
			}

			if (!modGardenProject.permissions().containsKey(modGardenUser.id())) {
				return new MessageResponse("You are not a member of project '" + project + "'.")
						.markEphemeral();
			}

			if (!modGardenProject.team().containsKey(invitedModGardenUser.id())) {
				return new MessageResponse(invitedUser.getGlobalName() + " is not a member of project '" + project + "'.")
						.markEphemeral();
			}

			long userPermissions = Long.parseLong(modGardenProject.permissions().get(modGardenUser.id()));
			if (!hasPermissions(userPermissions)) {
				return new MessageResponse("You are not allowed to invite users to project '" + project + "'.")
						.markEphemeral();
			}

			ProjectTeamPatch patch;

			if (permissions.equals(Permissions.NIL) && role != null) {
				patch = new ProjectTeamPatch(
						Map.of(invitedModGardenUser.id(), role),
						Collections.emptyMap()
				);
			} else if (!permissions.equals(Permissions.NIL) && role == null) {
				patch = new ProjectTeamPatch(
						Collections.emptyMap(),
						Map.of(invitedModGardenUser.id(), permissions.toString())
				);
			} else if (!permissions.equals(Permissions.NIL) && role != null){
				patch = new ProjectTeamPatch(
						Map.of(invitedModGardenUser.id(), role),
						Map.of(invitedModGardenUser.id(), permissions.toString())
				);
			} else {
				throw new NullPointerException("Either a role or permissions must be modified");
			}

			ModGarden.modifyTeamMembers(modGardenProject, patch);

			return new EmbedResponse()
					.setTitle("Successfully modified " + invitedUser.getGlobalName() + " in your project.")
					.setDescription(invitedUser.getGlobalName() + (role != null ? ", now has role " + role : "") + (!permissions.equals(Permissions.NIL) ? ", now has permissions " + String.join(", ", Permission.fromBigInteger(permissions, PermissionScope.USER).stream().map(Permission::getFriendlyName).toList()) : ""))
					.markEphemeral()
					.setColor(0xA9FFA7);
		} catch (Exception e) {
			GardenBot.LOG.error("", e);
			return new EmbedResponse()
					.setTitle("Encountered an exception!")
					.setDescription(e.getMessage() + "\nPlease report this to a team member.")
					.markEphemeral()
					.setColor(0xFF0000);
		}
	}

	@Override
	public List<Command.Choice> getAutoCompleteChoices(
			String focusedOption,
			User user,
			AutoCompletionGetter autoCompletionGetter
	) throws HypertextException {
		if (focusedOption.equals("project")) {
			return getEditableProjectAutoCompleteChoices(user);
		}

		return SlashCommandOption.pickChoices(
				() -> SlashCommandOption.getPermissionsChoices(autoCompletionGetter, "permissions", PermissionScope.PROJECT)
		);
	}
}
