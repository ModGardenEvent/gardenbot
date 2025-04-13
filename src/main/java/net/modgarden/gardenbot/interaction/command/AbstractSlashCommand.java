package net.modgarden.gardenbot.interaction.command;

import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.modgarden.gardenbot.interaction.InteractionHandler;
import net.modgarden.gardenbot.interaction.SlashCommandInteraction;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public abstract class AbstractSlashCommand implements InteractionHandler<SlashCommandInteraction> {
	public final String NAME;
	public final String DESCRIPTION;

	public AbstractSlashCommand(String name, String description) {
		NAME = name;
		DESCRIPTION = description;
	}

	public abstract SlashCommandData getData();

	public List<Command.Choice> getAutoCompleteChoices(String focusedOption, @Nullable String subcommandGroup, @Nullable String subcommandName) {
		return Collections.emptyList();
	}
}
