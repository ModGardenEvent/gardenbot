package net.modgarden.gardenbot.command.admin.role;

import java.math.BigInteger;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.modgarden.gardenbot.client.ModGarden;
import net.modgarden.gardenbot.client.exception.HypertextException;
import net.modgarden.gardenbot.client.mod_garden.role.ModGardenRole;
import net.modgarden.gardenbot.command.AutoCompletionGetter;
import net.modgarden.gardenbot.command.SlashCommandOption;
import net.modgarden.gardenbot.interaction.SlashCommandInteraction;
import net.modgarden.gardenbot.response.MessageResponse;
import net.modgarden.gardenbot.response.Response;
import net.modgarden.gardenbot.util.permission.PermissionScope;
import org.jetbrains.annotations.NotNull;

public class AdminRoleCopyCommand extends AdminRoleSlashCommand {
	public AdminRoleCopyCommand() {
		super(
				"copy",
				"Create a new role, copying the properties of the Discord role.",
				new SlashCommandOption(
						OptionType.ROLE,
						"discord_role",
						"The associated Discord role.",
						true
				),
				SlashCommandOption.permissions("permissions", true)
		);
	}

	@Override
	public List<Command.Choice> getAutoCompleteChoices(
			String focusedOption,
			User user,
			AutoCompletionGetter autoCompletionGetter
	) throws HypertextException {
		return SlashCommandOption.getPermissionsChoices(autoCompletionGetter, "permissions", PermissionScope.USER);
	}

	@NotNull
	@Override
	public Response respond(SlashCommandInteraction interaction) throws HypertextException, SQLException {
		interaction.event().deferReply(false).queue();

		Role discordRole = Objects.requireNonNull(interaction.event().getOption("discord_role")).getAsRole();
		String permissions = Objects.requireNonNull(interaction.event().getOption("permissions")).getAsString();
		BigInteger permissionBits = AdminRoleCreateCommand.parsePermissions(permissions);
		ModGardenRole role = ModGarden.createUserRoleFromDiscordRole(discordRole);
		ModGarden.modifyUserRole(role.id(), new ModGardenRole.Modifiable(null, permissionBits.toString(), null));

		return new MessageResponse("Created user role '" + role.name() + "' (`" + role.id() + "`)");
	}
}
