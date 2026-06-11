package net.modgarden.gardenbot.command.project;

import net.dv8tion.jda.api.entities.User;
import net.modgarden.gardenbot.command.SlashCommand;
import net.modgarden.gardenbot.interaction.SlashCommandInteraction;
import net.modgarden.gardenbot.response.Response;
import org.jetbrains.annotations.NotNull;

public class ProjectCreateCommand extends SlashCommand {
	public ProjectCreateCommand() {
		super(
				"create",
				"Creates an empty project. Which can be submitted to."
		);
	}

	@NotNull
	@Override
	public Response respond(SlashCommandInteraction interaction) {
		User user = interaction.event().getUser();
	}
}
