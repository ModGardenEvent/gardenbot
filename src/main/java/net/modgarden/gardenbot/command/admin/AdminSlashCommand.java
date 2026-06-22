package net.modgarden.gardenbot.command.admin;

import net.dv8tion.jda.api.entities.Role;
import net.modgarden.gardenbot.command.SlashCommand;
import net.modgarden.gardenbot.command.SlashCommandOption;
import net.modgarden.gardenbot.command.sudo.SudoCommand;
import net.modgarden.gardenbot.interaction.SlashCommandInteraction;
import net.modgarden.gardenbot.response.MessageResponse;
import net.modgarden.gardenbot.response.Response;

import java.util.List;

public abstract class AdminSlashCommand extends SlashCommand {

	public AdminSlashCommand(String name, String description, SlashCommandOption... options) {
		super(name, description, options);
	}

	@Override
	public final Response respondInternal(SlashCommandInteraction interaction) {
		if (interaction.event().getMember() == null || !hasPermission(interaction.event().getMember().getRoles())) {
			return new MessageResponse("You do not have the permissions to execute this command.");
		}

		return super.respondInternal(interaction);
	}

	private boolean hasPermission(List<Role> roles) {
		for (Role role : roles) {
			if (role.getId().equals("925706375034716211")) {
				return true;
			}
		}

		return false;
	}
}
