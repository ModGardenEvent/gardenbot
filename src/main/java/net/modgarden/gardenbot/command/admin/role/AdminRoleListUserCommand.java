package net.modgarden.gardenbot.command.admin.role;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.modgarden.gardenbot.GardenBot;
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
import net.modgarden.gardenbot.response.EmbedResponse;
import net.modgarden.gardenbot.response.MessageResponse;
import net.modgarden.gardenbot.response.Response;
import net.modgarden.gardenbot.util.permission.Permission;
import net.modgarden.gardenbot.util.permission.PermissionScope;
import net.modgarden.gardenbot.util.permission.Permissions;
import org.jetbrains.annotations.NotNull;

public class AdminRoleListUserCommand extends AdminRoleSlashCommand {
	public AdminRoleListUserCommand() {
		super(
				"list_user",
				"List user's roles.",
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

		StringBuilder descriptionBuilder = new StringBuilder();

		for (String roleId : user.roles()) {
			ModGardenRole role = ModGarden.getUserRole(roleId);

			if (role == null) {
				GardenBot.LOG.error("Role of ID '{}' does not exist, yet user '{}' ({}) has it!", roleId, user.username(), user.id());
				continue;
			}

			descriptionBuilder
					.append("**")
					.append(role.name())
					.append("** (`")
					.append(role.id())
					.append("`): ")
					.append(new Permissions(role.permissions()))
					.append('\n');
		}

		return new EmbedResponse()
				.setTitle("Roles of '" + user.username() + "' (`" + user.id() + "`)")
				.setColor(0xA6FFFE)
				.setDescription(descriptionBuilder.toString());
	}
}
