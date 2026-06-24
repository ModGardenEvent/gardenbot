package net.modgarden.gardenbot.command.admin.role;

import static net.modgarden.gardenbot.command.admin.role.AdminRoleRemoveUserCommand.checkRoleAbility;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.modgarden.gardenbot.client.Discord;
import net.modgarden.gardenbot.client.ModGarden;
import net.modgarden.gardenbot.client.exception.BadRequestException;
import net.modgarden.gardenbot.client.exception.HypertextException;
import net.modgarden.gardenbot.client.exception.NotFoundException;
import net.modgarden.gardenbot.client.mod_garden.role.ModGardenRole;
import net.modgarden.gardenbot.client.mod_garden.user.ModGardenUser;
import net.modgarden.gardenbot.command.AutoCompletionGetter;
import net.modgarden.gardenbot.command.SlashCommandOption;
import net.modgarden.gardenbot.command.admin.AdminSlashCommand;
import net.modgarden.gardenbot.interaction.SlashCommandInteraction;
import net.modgarden.gardenbot.response.MessageResponse;
import net.modgarden.gardenbot.response.Response;
import org.jetbrains.annotations.NotNull;

public class AdminRoleAddUserCommand extends AdminRoleSlashCommand {
	public AdminRoleAddUserCommand() {
		super(
				"add_user",
				"Add a role to a user.",
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

		ModGarden.addUserRole(user, role);

		if (discordUser != null && role.integrations().discord() != null) {
			Member member = Objects.requireNonNull(interaction.event().getGuild())
					.retrieveMember(discordUser)
					.complete();
			Discord.addModGardenRolesToDiscordUser(interaction.event().getGuild(), member);
		}

		return new MessageResponse("Added role '" + role.name() + "' to '" + user.username() + "' (`" + user.id() + "`)");
	}
}
