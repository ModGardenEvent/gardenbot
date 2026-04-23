package net.modgarden.gardenbot.interaction.dispatcher;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.modgarden.gardenbot.command.AbstractSlashCommand;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.modgarden.gardenbot.GardenBot;
import net.modgarden.gardenbot.interaction.SlashCommandInteraction;
import net.modgarden.gardenbot.response.Response;

import java.util.HashMap;
import java.util.List;

public class SlashCommandDispatcher {
	private static final HashMap<String, AbstractSlashCommand> COMMANDS = new HashMap<>();

	public static void register(AbstractSlashCommand slashCommand) {
		COMMANDS.put(slashCommand.name, slashCommand);
	}

	public static List<Command.Choice> getAutoCompleteChoices(CommandAutoCompleteInteractionEvent event) {
		var slashCommand = COMMANDS.get(event.getName());
		return slashCommand.getAutoCompleteChoices(event.getFocusedOption().getName(), event.getUser(), event::getOption, event.getSubcommandGroup(), event.getSubcommandName());
	}

	public static void apply(CommandListUpdateAction action) {
		action.addCommands(COMMANDS.values().stream().map(AbstractSlashCommand::asData).toList()).queue();
	}

	public static Response dispatch(SlashCommandInteraction command) {
		var slashCommand = COMMANDS.get(command.event().getName());
		return slashCommand.respond(command);
	}

	public static void addCommands(Guild guild) {
		if (Boolean.parseBoolean(GardenBot.DOTENV.get("GARDENBOT_UPSERT_COMMANDS", "false"))) {
			List<SlashCommandData> commandData = COMMANDS.values()
					.stream()
					.map(AbstractSlashCommand::getData)
					.toList();

			guild.updateCommands()
					.addCommands(commandData)
					.complete();

			GardenBot.LOG.info("Successfully upserted GardenBot commands!");
		}
	}
}
