package net.modgarden.gardenbot.command.admin.role;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.modgarden.gardenbot.client.ModGarden;
import net.modgarden.gardenbot.client.exception.BadRequestException;
import net.modgarden.gardenbot.client.exception.HypertextException;
import net.modgarden.gardenbot.client.exception.NotFoundException;
import net.modgarden.gardenbot.client.mod_garden.role.ModGardenRole;
import net.modgarden.gardenbot.client.mod_garden.user.ModGardenUser;
import net.modgarden.gardenbot.command.AutoCompletionGetter;
import net.modgarden.gardenbot.command.SlashCommandOption;
import net.modgarden.gardenbot.command.admin.AdminSlashCommand;
import net.modgarden.gardenbot.command.sudo.SudoCommand;
import net.modgarden.gardenbot.interaction.SlashCommandInteraction;
import net.modgarden.gardenbot.response.MessageResponse;
import net.modgarden.gardenbot.response.Response;
import net.modgarden.gardenbot.util.permission.Permission;
import net.modgarden.gardenbot.util.permission.Permissions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AdminRoleRemoveUserCommand extends AdminRoleSlashCommand {
	public AdminRoleRemoveUserCommand() {
		super(
				"remove_user",
				"Remove a role from a user.",
				SlashCommandOption.role("role", true),
				new SlashCommandOption(
						OptionType.STRING,
						"user_id",
						"The user's ID.",
						false
				),
				new SlashCommandOption(
						OptionType.STRING,
						"username",
						"The user's username.",
						false
				),
				new SlashCommandOption(
						OptionType.USER,
						"discord_user",
						"The Discord user.",
						false
				)
		);
	}

	@Override
	public List<Command.Choice> getAutoCompleteChoices(
			String focusedOption,
			User user,
			AutoCompletionGetter autoCompletionGetter
	) throws HypertextException {
		return SlashCommandOption.getRoleIdChoices(focusedOption, autoCompletionGetter, "role");
	}

	@NotNull
	@Override
	public Response respond(SlashCommandInteraction interaction) throws HypertextException, SQLException {
		interaction.event().deferReply(true).queue();

		String roleId = interaction.event().getOption("role", OptionMapping::getAsString);
		ModGardenRole role = ModGarden.getUserRole(roleId);

		if (role == null) {
			throw new NotFoundException("Role of ID '" + roleId + "' does not exist");
		}

		String userId = interaction.event().getOption("user_id", OptionMapping::getAsString);
		String username = interaction.event().getOption("username", OptionMapping::getAsString);
		User discordUser = interaction.event().getOption("discord_user", OptionMapping::getAsUser);
		ModGardenUser user;

		if (userId != null) {
			user = ModGarden.getUserByModGardenId(userId);

			if (user == null) {
				throw new NotFoundException("User by ID '" + userId + "' does not exist");
			}
		} else if (username != null) {
			user = ModGarden.getUserByModGardenUsername(username);

			if (user == null) {
				throw new NotFoundException("User by username '" + username + "' does not exist");
			}
		} else if (discordUser != null) {
			user = ModGarden.getUserByDiscordUser(discordUser);

			if (user == null) {
				throw new NotFoundException("User '" + discordUser.getName() + "' does not have a Mod Garden account");
			}
		} else {
			throw new BadRequestException("No user was specified");
		}

		MessageResponse x = checkRoleAbility(interaction, role);
		if (x != null) return x;

		ModGarden.removeUserRole(user, role);

		if (discordUser != null && role.integrations().discord() != null) {
			Guild guild = Objects.requireNonNull(interaction.event().getGuild());
			guild.removeRoleFromMember(
					discordUser,
					Objects.requireNonNull(guild.getRoleById(role.integrations().discord().roleId()))
			).complete();
		}

		return new MessageResponse("Removed role '" + role.name() + "' from '" + user.username() + "' (" + user.id() + ")");
	}

	@Nullable
	public static MessageResponse checkRoleAbility(
			SlashCommandInteraction interaction,
			ModGardenRole role
	) throws HypertextException {
		Member member = Objects.requireNonNull(interaction.event().getMember());
		Guild guild = Objects.requireNonNull(interaction.event().getGuild());
		ModGardenUser user = ModGarden.getUserByDiscordUser(member.getUser());

		boolean isSudo = member.getRoles().contains(guild.getRoleById(SudoCommand.SUDO_ROLE_ID));
		boolean modifyingAdminButNotAdmin;
		boolean modifyingRoleAboveMe = !isSudo && (role.integrations().discord() != null && member.canInteract(Objects.requireNonNull(guild.getRoleById(role.integrations().discord().roleId()))));

		if (user != null) {
			modifyingAdminButNotAdmin = !isSudo && (new Permissions(role.permissions()).hasPermissions(Permission.ADMINISTRATOR) && !new Permissions(user.permissions()).hasPermissions(Permission.ADMINISTRATOR));
		} else {
			modifyingAdminButNotAdmin = !isSudo && new Permissions(role.permissions()).hasPermissions(Permission.ADMINISTRATOR);
		}

		if (modifyingAdminButNotAdmin || modifyingRoleAboveMe) {
			return new MessageResponse("You do not have permission to execute this command.");
		}

		return null;
	}
}
