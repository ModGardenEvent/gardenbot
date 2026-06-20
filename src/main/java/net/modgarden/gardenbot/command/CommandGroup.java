package net.modgarden.gardenbot.command;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import net.modgarden.gardenbot.interaction.SlashCommandInteraction;
import net.modgarden.gardenbot.response.Response;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public abstract class CommandGroup<T extends AbstractSlashCommand> extends AbstractSlashCommand {
	private final Map<String, ? extends T> subCommands;
	private final boolean isGroup;

	@SafeVarargs
	public CommandGroup(String name,
                        String description,
                        Supplier<T>... subCommands) {
		super(name, description);
		this.subCommands = Arrays.stream(subCommands)
				.map(supplier -> {
					T subCommand = supplier.get();
					return Map.entry(subCommand.name, subCommand);
				})
				.collect(
						Collectors.toMap(
								Map.Entry::getKey,
								Map.Entry::getValue,
								(_, newMap) -> newMap
						)
				);
		this.isGroup = subCommands.length > 0 && subCommands[0] instanceof CommandGroup<?>;
		validateSubCommandsOrThrow();
	}

	@NotNull
	@Override
	public Response respond(SlashCommandInteraction interaction) {
		String lookupName = isGroup
				? interaction.event().getSubcommandGroup()
				: interaction.event().getSubcommandName();

		if (interaction.event().getSubcommandName() == null || !subCommands.containsKey(lookupName)) {
			throw new NullPointerException("Could not find subcommand '" + lookupName + "' for command '" + name + "'");
		}

		AbstractSlashCommand subCommand = subCommands.get(lookupName);
		return subCommand.respond(interaction);
	}

	@Override
	public List<Command.Choice> getAutoCompleteChoices(String focusedOption,
	                                                   User user,
	                                                   AutoCompletionGetter autoCompletionGetter,
	                                                   @Nullable String groupName,
	                                                   @Nullable String subCommandName) {
		String lookupName = isGroup
				? groupName
				: subCommandName;

		if (!subCommands.containsKey(lookupName)) {
			return Collections.emptyList();
		}
		T subCommand = subCommands.get(lookupName);
		return subCommand.getAutoCompleteChoices(focusedOption, user, autoCompletionGetter, groupName, subCommandName);
	}

	@Override
	public SlashCommandData asData() {
		SlashCommandData data = Commands.slash(name, description);
		addSubCommandData(data);
		addSubCommandGroupData(data);
		return data;
	}

	protected SubcommandGroupData asGroupData() {
		SubcommandGroupData data = new SubcommandGroupData(name, description);
		addSubCommandData(data);
		return data;
	}

	@SuppressWarnings("DuplicatedCode")
	private void addSubCommandData(SlashCommandData data) {
		List<SubcommandData> subCommandData = subCommands.values()
				.stream()
				.map(command -> {
					if (command instanceof SlashCommand slashCommand) {
						return slashCommand.asSubCommandData();
					}
					return null;
				})
				.filter(Objects::nonNull)
				.toList();
		if (!subCommandData.isEmpty()) {
			data.addSubcommands(subCommandData);
		}
	}

	@SuppressWarnings("DuplicatedCode")
	private void addSubCommandData(SubcommandGroupData data) {
		List<SubcommandData> subCommandData = subCommands.values()
				.stream()
				.map(command -> {
					if (command instanceof SlashCommand slashCommand) {
						return slashCommand.asSubCommandData();
					}
					return null;
				})
				.filter(Objects::nonNull)
				.toList();
		if (!subCommandData.isEmpty()) {
			data.addSubcommands(subCommandData);
		}
	}

	private void addSubCommandGroupData(SlashCommandData data) {
		List<SubcommandGroupData> groupData = subCommands.values()
				.stream()
				.map(command -> {
					if (command instanceof CommandGroup<?> groupCommand) {
						return groupCommand.asGroupData();
					}
					return null;
				})
				.filter(Objects::nonNull)
				.toList();
		if (!groupData.isEmpty()) {
			data.addSubcommandGroups(groupData);
		}
	}

	private void validateSubCommandsOrThrow() {
		validateSubCommandsOrThrow(0);
	}

	private void validateSubCommandsOrThrow(int recursionIndex) {
		if (subCommands.isEmpty()) {
			throw new IllegalArgumentException("CommandGroup must have at least one subcommand or subgroup");
		}

		if (recursionIndex > 2) {
			throw new IllegalArgumentException("Discord does not allow subcommand groups inside of subcommand groups");
		}

		for (T subCommand : subCommands.values()) {
			if (subCommand instanceof CommandGroup<?> commandGroup) {
				commandGroup.validateSubCommandsOrThrow(recursionIndex + 1);
			}
		}
	}
}
