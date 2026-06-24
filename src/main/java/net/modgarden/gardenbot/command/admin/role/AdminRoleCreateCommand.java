package net.modgarden.gardenbot.command.admin.role;

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
import net.modgarden.gardenbot.client.mod_garden.role.ModGardenRole;
import net.modgarden.gardenbot.client.mod_garden.role.RoleIntegrations;
import net.modgarden.gardenbot.client.mod_garden.role.integration.DiscordRoleIntegration;
import net.modgarden.gardenbot.command.AutoCompletionGetter;
import net.modgarden.gardenbot.command.SlashCommandOption;
import net.modgarden.gardenbot.command.admin.AdminSlashCommand;
import net.modgarden.gardenbot.interaction.SlashCommandInteraction;
import net.modgarden.gardenbot.response.MessageResponse;
import net.modgarden.gardenbot.response.Response;
import net.modgarden.gardenbot.util.permission.Permission;
import org.jetbrains.annotations.NotNull;

public class AdminRoleCreateCommand extends AdminRoleSlashCommand {
	public AdminRoleCreateCommand() {
		super(
				"create",
				"Create a new role.",
				new SlashCommandOption(
						OptionType.STRING,
						"name",
						"The name of the role.",
						true
				),
				SlashCommandOption.permissions("permissions", true),
				new SlashCommandOption(
						OptionType.ROLE,
						"discord_role",
						"The associated Discord role.",
						false
				)
		);
	}

	@NotNull
	public static BigInteger parsePermissions(String permissions) {
		BigInteger permissionBits = BigInteger.valueOf(0);
		String[] splitPermissions = permissions.split(",");

		for (String permission : splitPermissions) {
			if (permission.startsWith(" ")) {
				permission = permission.replaceFirst(" ", "");
			}

			permissionBits = permissionBits.or(BigInteger.valueOf(Permission.fromName(permission).getBit()));
		}
		return permissionBits;
	}

	@Override
	public List<Command.Choice> getAutoCompleteChoices(
			String focusedOption,
			User user,
			AutoCompletionGetter autoCompletionGetter
	) throws HypertextException {
		return SlashCommandOption.getPermissionsChoices(autoCompletionGetter, "permissions");
	}

	@NotNull
	@Override
	public Response respond(SlashCommandInteraction interaction) throws HypertextException, SQLException {
		interaction.event().deferReply(false).queue();
		String name = interaction.event().getOption("name", OptionMapping::getAsString);
		String permissions = interaction.event().getOption("permissions", OptionMapping::getAsString);
		Role discordRole = interaction.event().getOption("discord_role", OptionMapping::getAsRole);
		BigInteger permissionBits = parsePermissions(Objects.requireNonNull(permissions));

		DiscordRoleIntegration discordRoleIntegration;

		if (discordRole != null) {
			discordRoleIntegration = new DiscordRoleIntegration(discordRole.getId());
		} else {
			discordRoleIntegration = null;
		}

		ModGardenRole role = ModGarden.createUserRole(
				name,
				permissionBits.toString(),
				new RoleIntegrations(discordRoleIntegration)
		);

		return new MessageResponse("Created user role '" + role.name() + "' (`" + role.id() + "`)");
	}
}
