package net.modgarden.gardenbot.interaction.command;

import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import net.modgarden.gardenbot.interaction.InteractionHandler;
import net.modgarden.gardenbot.interaction.SlashCommandInteraction;
import net.modgarden.gardenbot.interaction.response.Response;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class SubCommandSlashCommand extends AbstractSlashCommand {
	private final Map<String, SubCommand> SUBCOMMANDS;

	public SubCommandSlashCommand(String name, String description, SubCommand... subCommands) {
		super(name, description);
		this.SUBCOMMANDS = Arrays.stream(subCommands)
				.map(subCommand -> Pair.of(subCommand.NAME, subCommand))
				.collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
	}

	@NotNull
	@Override
	public Response respond(SlashCommandInteraction interaction) {
		return SUBCOMMANDS.get(interaction.event().getSubcommandName()).HANDLER.respond(interaction);
	}

	@Override
	public SlashCommandData getData() {
		var command = Commands.slash(NAME, DESCRIPTION);
		command.addSubcommands(SUBCOMMANDS.values().stream().map(SubCommand::getData).toArray(SubcommandData[]::new));
		return command;
	}

	public static class SubCommand {
		public final String NAME;
		public final String DESCRIPTION;
		private final InteractionHandler<SlashCommandInteraction> HANDLER;
		public final SlashCommandOption[] OPTIONS;

		public SubCommand(String name,
						  String description,
						  InteractionHandler<SlashCommandInteraction> handler,
						  SlashCommandOption... options) {
			NAME = name;
			DESCRIPTION = description;
			HANDLER = handler;
			OPTIONS = options;
		}

		@NotNull
		public Response respond(SlashCommandInteraction interaction) {
			return HANDLER.respond(interaction);
		}

		SubcommandData getData() {
			var command = new SubcommandData(NAME, DESCRIPTION);

			for (var option : OPTIONS)
				command.addOption(option.type(), option.name(), option.description(), option.required());

			return command;
		}
	}
}
