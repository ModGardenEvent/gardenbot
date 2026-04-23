package net.modgarden.gardenbot.command;

import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public record SlashCommandOption(OptionType type,
								 String name,
								 String description,
								 boolean required,
								 boolean isAutoComplete) {
	public SlashCommandOption(OptionType type,
	                          String name,
	                          String description,
	                          boolean required) {
		this(type, name, description, required, false);
	}

	public OptionData getData() {
		OptionData data = new OptionData(type, name, description);
		data.setRequired(required);
		data.setAutoComplete(isAutoComplete);
		return data;
	}
}
