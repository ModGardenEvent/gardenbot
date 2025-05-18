package net.modgarden.gardenbot.interaction.command;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.modgarden.gardenbot.interaction.SlashCommandInteraction;
import net.modgarden.gardenbot.interaction.response.Response;

import java.util.HashMap;
import java.util.List;

public class SlashCommandDispatcher {
	private static final HashMap<String, AbstractSlashCommand> COMMANDS = new HashMap<>();

	public static void register(AbstractSlashCommand slashCommand) {
		COMMANDS.put(slashCommand.NAME, slashCommand);
	}

	public static List<Command.Choice> getAutoCompleteChoices(CommandAutoCompleteInteractionEvent event) {
		var slashCommand = COMMANDS.get(event.getName());
		return slashCommand.getAutoCompleteChoices(event.getFocusedOption().getName(), event.getSubcommandGroup(), event.getSubcommandName());
	}

	public static void apply(CommandListUpdateAction action) {
		action.addCommands(COMMANDS.values().stream().map(AbstractSlashCommand::getData).toList()).queue();
	}

	public static Response dispatch(SlashCommandInteraction command) {
		var slashCommand = COMMANDS.get(command.event().getName());
		return slashCommand.respond(command);
	}

	public static void addCommands(Guild guild) {
		// TODO: Only upsert when a commit has [upsert] in its name if possible.
		guild.updateCommands().addCommands(COMMANDS.values().stream().map(AbstractSlashCommand::getData).toList()).complete();
	}
}
