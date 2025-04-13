package net.modgarden.gardenbot.interaction.command;

import net.dv8tion.jda.api.interactions.commands.OptionType;

public record SlashCommandOption(OptionType type, String name, String description, boolean required, boolean isAutoComplete) {
	public SlashCommandOption(OptionType type, String name, String description, boolean required) {
		this(type, name, description, required, false);
	}
}
