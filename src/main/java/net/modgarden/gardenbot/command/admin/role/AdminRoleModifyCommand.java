package net.modgarden.gardenbot.command.admin.role;

import static net.modgarden.gardenbot.command.admin.role.AdminRoleRemoveUserCommand.checkRoleAbility;

import java.math.BigInteger;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.modgarden.gardenbot.client.ModGarden;
import net.modgarden.gardenbot.client.exception.HypertextException;
import net.modgarden.gardenbot.client.exception.InternalServerException;
import net.modgarden.gardenbot.client.exception.NotFoundException;
import net.modgarden.gardenbot.client.mod_garden.role.ModGardenRole;
import net.modgarden.gardenbot.client.mod_garden.role.RoleIntegrations;
import net.modgarden.gardenbot.client.mod_garden.role.integration.DiscordRoleIntegration;
import net.modgarden.gardenbot.command.AutoCompletionGetter;
import net.modgarden.gardenbot.command.SlashCommandOption;
import net.modgarden.gardenbot.command.admin.AdminSlashCommand;
import net.modgarden.gardenbot.interaction.SlashCommandInteraction;
import net.modgarden.gardenbot.response.MessageResponse;
import net.modgarden.gardenbot.response.Response;
import net.modgarden.gardenbot.util.NullableWrapper;
import org.jetbrains.annotations.NotNull;

public class AdminRoleModifyCommand extends AdminRoleSlashCommand {
	public AdminRoleModifyCommand() {
		super(
				"modify",
				"Modify an existing user role's properties.",
				SlashCommandOption.role("role", true),
				new SlashCommandOption(
						OptionType.STRING,
						"name",
						"The role name.",
						false
				),
				SlashCommandOption.permissions("permissions", false),
				new SlashCommandOption(
						OptionType.ROLE,
						"discord_role",
						"The associated Discord role.",
						false
				),
				new SlashCommandOption(
						OptionType.BOOLEAN,
						"remove_discord_role",
						"Whether to remove the Discord role.",
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
		return SlashCommandOption.pickChoices(
				() -> SlashCommandOption.getRoleIdChoices(focusedOption, autoCompletionGetter, "role"),
				() -> SlashCommandOption.getPermissionsChoices(autoCompletionGetter, "permissions")
		);
	}

	@NotNull
	@Override
	public Response respond(SlashCommandInteraction interaction) throws HypertextException, SQLException {
		interaction.event().deferReply(false).queue();

		String roleId = interaction.event().getOption("role", OptionMapping::getAsString);
		String name = interaction.event().getOption("name", OptionMapping::getAsString);
		Role discordRole = interaction.event().getOption("discord_role", OptionMapping::getAsRole);
		boolean removeDiscordRole = interaction.event().getOption("remove_discord_role", false, OptionMapping::getAsBoolean);
		String permissions = interaction.event().getOption("permissions", OptionMapping::getAsString);
		BigInteger permissionBits;

		if (permissions != null) {
			permissionBits = AdminRoleCreateCommand.parsePermissions(permissions);
		} else {
			permissionBits = BigInteger.ZERO;
		}

		ModGardenRole role = ModGarden.getUserRole(roleId);

		if (role == null) {
			throw new NotFoundException("Role of ID '" + roleId + "' does not exist");
		}

		NullableWrapper<DiscordRoleIntegration> discordRoleIntegration;

		if (removeDiscordRole) {
			discordRoleIntegration = NullableWrapper.empty();
		} else if (discordRole != null) {
			discordRoleIntegration = NullableWrapper.of(new DiscordRoleIntegration(discordRole.getId()));
		} else {
			discordRoleIntegration = null;
		}

		MessageResponse x = checkRoleAbility(interaction, role);
		if (x != null) return x;

		ModGarden.modifyUserRole(role.id(), new ModGardenRole.Modifiable(name, permissionBits.toString(), new RoleIntegrations.Modifiable(discordRoleIntegration)));

		return new MessageResponse("Modified user role '" + role.name() + "' (`" + role.id() + "`)");
	}
}
