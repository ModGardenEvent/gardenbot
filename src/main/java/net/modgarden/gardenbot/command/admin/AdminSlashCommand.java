package net.modgarden.gardenbot.command.admin;

import net.dv8tion.jda.api.entities.Role;
import net.modgarden.gardenbot.GardenBot;
import net.modgarden.gardenbot.client.ModGarden;
import net.modgarden.gardenbot.client.exception.HypertextException;
import net.modgarden.gardenbot.client.mod_garden.user.ModGardenUser;
import net.modgarden.gardenbot.command.SlashCommand;
import net.modgarden.gardenbot.command.SlashCommandOption;
import net.modgarden.gardenbot.interaction.SlashCommandInteraction;
import net.modgarden.gardenbot.response.MessageResponse;
import net.modgarden.gardenbot.response.Response;
import net.modgarden.gardenbot.util.permission.PermissionPredicate;
import net.modgarden.gardenbot.util.permission.Permissions;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static net.modgarden.gardenbot.GardenBot.IS_DEV_ENV;
import static net.modgarden.gardenbot.command.sudo.SudoCommand.SUDO_ROLE_ID;

public abstract class AdminSlashCommand extends SlashCommand {
	public AdminSlashCommand(String name, String description, SlashCommandOption... options) {
		super(name, description, options);
	}

	protected @Nullable PermissionPredicate requiredPermissions() {
		return null;
	}

	@Override
	public final Response respondInternal(SlashCommandInteraction interaction) throws HypertextException {
		PermissionPredicate permissionPredicate = this.requiredPermissions();

		if (permissionPredicate != null && interaction.event().getMember() != null) {
			ModGardenUser user = ModGarden.getUserByDiscordUser(interaction.event().getUser());

			if (user != null) {
				permissionPredicate.test(new Permissions(user.permissions()));
			}
		}

		if (interaction.event().getMember() == null || !hasPermission(interaction.event().getMember().getRoles())) {
			return new MessageResponse("You do not have the permissions to execute this command.");
		}

		return super.respondInternal(interaction);
	}

	private boolean hasPermission(List<Role> roles) {
		for (Role role : roles) {
			if (role.getId().equals(SUDO_ROLE_ID) || IS_DEV_ENV) {
				return true;
			}
		}

		return false;
	}
}
