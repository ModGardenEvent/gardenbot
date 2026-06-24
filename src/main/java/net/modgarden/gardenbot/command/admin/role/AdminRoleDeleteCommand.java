package net.modgarden.gardenbot.command.admin.role;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.modgarden.gardenbot.client.ModGarden;
import net.modgarden.gardenbot.client.exception.HypertextException;
import net.modgarden.gardenbot.client.mod_garden.role.ModGardenRole;
import net.modgarden.gardenbot.command.AutoCompletionGetter;
import net.modgarden.gardenbot.command.SlashCommandOption;
import net.modgarden.gardenbot.command.admin.AdminSlashCommand;
import net.modgarden.gardenbot.interaction.SlashCommandInteraction;
import net.modgarden.gardenbot.response.MessageResponse;
import net.modgarden.gardenbot.response.Response;
import net.modgarden.gardenbot.util.permission.PermissionPredicate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AdminRoleDeleteCommand extends AdminRoleSlashCommand {
	public AdminRoleDeleteCommand() {
		super(
				"delete",
				"Delete a role.",
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
		interaction.event().deferReply(false).queue();
		String id = interaction.event().getOption("role", OptionMapping::getAsString);
		ModGardenRole userRole = ModGarden.getUserRole(id);
		ModGarden.deleteUserRole(id);
		return new MessageResponse("Deleted user role " + Objects.requireNonNull(userRole).name());
	}

	@Nullable
	@Override
	protected PermissionPredicate requiredPermissions() {
		return null;
	}
}
