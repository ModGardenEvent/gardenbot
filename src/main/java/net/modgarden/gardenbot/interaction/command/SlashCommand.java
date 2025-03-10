package net.modgarden.gardenbot.interaction.command;

import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.modgarden.gardenbot.interaction.InteractionHandler;
import net.modgarden.gardenbot.interaction.response.EmbedResponse;
import net.modgarden.gardenbot.interaction.SlashCommandInteraction;
import net.modgarden.gardenbot.interaction.response.Response;
import org.jetbrains.annotations.NotNull;

public class SlashCommand extends AbstractSlashCommand {
	private final InteractionHandler<SlashCommandInteraction> HANDLER;
	public final SlashCommandOption[] OPTIONS;

	public SlashCommand(String name, String description, InteractionHandler<SlashCommandInteraction> handler, SlashCommandOption... options) {
		super(name, description);
		HANDLER = handler;
		OPTIONS = options;
	}

	@NotNull
	@Override
	public Response respond(SlashCommandInteraction interaction) {
		return HANDLER.respond(interaction);
	}

	@Override
	public SlashCommandData getData() {
		var command = Commands.slash(NAME, DESCRIPTION);

		for (var option : OPTIONS)
			command.addOption(option.type(), option.name(), option.description(), option.required());

		return command;
	}
}
