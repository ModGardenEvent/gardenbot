package net.modgarden.gardenbot.interaction.command;

import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.modgarden.gardenbot.interaction.response.EmbedResponse;
import net.modgarden.gardenbot.interaction.SlashCommandInteraction;
import net.modgarden.gardenbot.interaction.response.Response;

import java.util.HashMap;

public class SlashCommandDispatcher {
	private static final HashMap<String, AbstractSlashCommand> COMMANDS = new HashMap<>();

	public static void register(AbstractSlashCommand slashCommand) {
		COMMANDS.put(slashCommand.NAME, slashCommand);
	}

	public static void apply(CommandListUpdateAction action) {
		action.addCommands(COMMANDS.values().stream().map(AbstractSlashCommand::getData).toList()).queue();
	}

	public static Response dispatch(SlashCommandInteraction command) {
		var slashCommand = COMMANDS.get(command.event().getName());
		return slashCommand.respond(command);
	}
}
