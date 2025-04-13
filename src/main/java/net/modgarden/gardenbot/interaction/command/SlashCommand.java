package net.modgarden.gardenbot.interaction.command;

import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import net.modgarden.gardenbot.interaction.InteractionHandler;
import net.modgarden.gardenbot.interaction.SlashCommandInteraction;
import net.modgarden.gardenbot.interaction.response.Response;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SlashCommand extends AbstractSlashCommand {
	@Nullable
	private final InteractionHandler<SlashCommandInteraction> HANDLER;
	public final SlashCommandOption[] OPTIONS;
	@Nullable
	public final AutoCompleteFunction COMPLETION_FUNCTION;
	public final Map<String, SubCommand> SUBCOMMANDS;

	public SlashCommand(String name, String description, InteractionHandler<SlashCommandInteraction> handler, @Nullable AutoCompleteFunction completionFunction, SlashCommandOption... options) {
		super(name, description);
		HANDLER = handler;
		OPTIONS = options;
		COMPLETION_FUNCTION = completionFunction;
		SUBCOMMANDS = Map.of();
	}

	public SlashCommand(String name, String description, SubCommand... subCommands) {
		super(name, description);
		HANDLER = null;
		OPTIONS = new SlashCommandOption[0];
		COMPLETION_FUNCTION = null;
		SUBCOMMANDS = Arrays.stream(subCommands)
				.map(subCommand -> Pair.of(subCommand.NAME, subCommand))
				.collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
	}

	@NotNull
	@Override
	public Response respond(SlashCommandInteraction interaction) {
		if (interaction.event().getSubcommandName() != null && SUBCOMMANDS.containsKey(interaction.event().getSubcommandName()))
			return SUBCOMMANDS.get(interaction.event().getSubcommandName()).respond(interaction);

		if (HANDLER != null)
			return HANDLER.respond(interaction);

		throw new UnsupportedOperationException("Invalid slash command response. Make sure the handler or subcommand you are trying to access exists.");
	}

	@Override
	public SlashCommandData getData() {
		var command = Commands.slash(NAME, DESCRIPTION);

		if (OPTIONS.length > 0 && !SUBCOMMANDS.isEmpty())
			throw new UnsupportedOperationException("Discord does not support root commands with options.");

		for (var option : OPTIONS)
			command.addOption(option.type(), option.name(), option.description(), option.required(), option.isAutoComplete());

		for (var subcommand : SUBCOMMANDS.values())
			command.addSubcommands(subcommand.getData());

		return command;
	}

	@Override
	public List<Command.Choice> getAutoCompleteChoices(String focusedOption, @Nullable String subcommandGroup, @Nullable String subcommandName) {
		if (!SUBCOMMANDS.isEmpty()) {
			SubCommand subCommand = SUBCOMMANDS.get(subcommandName);
			if (subCommand != null && subCommand.COMPLETION_FUNCTION != null)
				return subCommand.COMPLETION_FUNCTION.autoCompleteChoices(focusedOption);
			return SUBCOMMANDS.get(subcommandGroup).SUBCOMMANDS.get(subcommandName).COMPLETION_FUNCTION.autoCompleteChoices(focusedOption);
		}
		return COMPLETION_FUNCTION != null ? COMPLETION_FUNCTION.autoCompleteChoices(focusedOption) : Collections.emptyList();
	}

	public static class SubCommand {
		public final String NAME;
		public final String DESCRIPTION;
		private final InteractionHandler<SlashCommandInteraction> HANDLER;
		@Nullable
		public final AutoCompleteFunction COMPLETION_FUNCTION;
		public final SlashCommandOption[] OPTIONS;
		public final Map<String, SubCommand> SUBCOMMANDS;

		public SubCommand(String name,
						  String description,
						  InteractionHandler<SlashCommandInteraction> handler,
						  SubCommand... subCommands) {
			NAME = name;
			DESCRIPTION = description;
			HANDLER = handler;
			COMPLETION_FUNCTION = null;
			OPTIONS = new SlashCommandOption[0];
			SUBCOMMANDS = Arrays.stream(subCommands)
					.map(subCommand -> Pair.of(subCommand.NAME, subCommand))
					.collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
		}

		public SubCommand(String name,
						  String description,
						  InteractionHandler<SlashCommandInteraction> handler,
						  @Nullable AutoCompleteFunction completionFunction,
						  SlashCommandOption... options) {
			NAME = name;
			DESCRIPTION = description;
			HANDLER = handler;
			COMPLETION_FUNCTION = completionFunction;
			OPTIONS = options;
			SUBCOMMANDS = Collections.emptyMap();
		}

		@NotNull
		public Response respond(SlashCommandInteraction interaction) {
			return HANDLER.respond(interaction);
		}

		SubcommandData getData() {
			var command = new SubcommandData(NAME, DESCRIPTION);

			for (var option : OPTIONS)
				command.addOption(option.type(), option.name(), option.description(), option.required(), option.isAutoComplete());

			return command;
		}
	}

	@FunctionalInterface
	public interface AutoCompleteFunction {
		List<Command.Choice> autoCompleteChoices(String focusedOption);
	}
}
