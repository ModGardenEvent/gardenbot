package net.modgarden.gardenbot.interaction.command;

import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.modgarden.gardenbot.interaction.InteractionHandler;
import net.modgarden.gardenbot.interaction.SlashCommandInteraction;

public abstract class AbstractSlashCommand implements InteractionHandler<SlashCommandInteraction> {
	public final String NAME;
	public final String DESCRIPTION;

	public AbstractSlashCommand(String name, String description) {
		NAME = name;
		DESCRIPTION = description;
	}

	public abstract SlashCommandData getData();
}
