package net.modgarden.gardenbot.command.admin.role;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

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
import net.modgarden.gardenbot.interaction.SlashCommandInteraction;
import net.modgarden.gardenbot.response.EmbedResponse;
import net.modgarden.gardenbot.response.MessageResponse;
import net.modgarden.gardenbot.response.Response;
import net.modgarden.gardenbot.util.permission.Permission;
import net.modgarden.gardenbot.util.permission.PermissionScope;
import net.modgarden.gardenbot.util.permission.Permissions;
import org.jetbrains.annotations.NotNull;

public class AdminRoleInfoCommand extends AdminSlashCommand {
	public AdminRoleInfoCommand() {
		super(
				"info",
				"Get information about a role.",
				SlashCommandOption.role("role", true)
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

		StringBuilder descriptionBuilder = new StringBuilder();
		descriptionBuilder
				.append("**Created:** ")
				.append("<t:")
				.append(Instant.ofEpochMilli(Long.parseLong(role.created())).getEpochSecond())
				.append(":S>")
				.append("\n**Permissions:** ")
				.append(new Permissions(role.permissions()));

		if (role.integrations().discord() != null) {
			descriptionBuilder.append("\n**Discord Role:** ")
					.append("<@&")
					.append(role.integrations().discord().roleId())
					.append(">");
		}

		return new EmbedResponse()
				.setTitle(role.name() + " (`" + role.id() + "`)")
				.setColor(0xA6FFFE)
				.setDescription(descriptionBuilder.toString());
	}
}
