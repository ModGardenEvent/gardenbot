package net.modgarden.gardenbot.command;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.modgarden.gardenbot.client.exception.HypertextException;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class SlashCommand extends AbstractSlashCommand {
	public final SlashCommandOption[] options;

	public SlashCommand(String name, String description, SlashCommandOption... options) {
		super(name, description);
		this.options = options;
	}

	public List<Command.Choice> getAutoCompleteChoices(String focusedOption, User user, AutoCompletionGetter autoCompletionGetter) throws HypertextException {
		return Collections.emptyList();
	}

	@Override
	public final List<Command.Choice> getAutoCompleteChoices(String focusedOption, User user, AutoCompletionGetter autoCompletionGetter, @Nullable String groupName, @Nullable String subCommandName) throws HypertextException {
		return getAutoCompleteChoices(focusedOption, user, autoCompletionGetter);
	}

	@Override
	public SlashCommandData asData() {
		SlashCommandData data = Commands.slash(name, description);
		data.addOptions(
				Arrays.stream(options)
						.map(SlashCommandOption::getData)
						.toList()
		);
		return data;
	}

	protected SubcommandData asSubCommandData() {
		SubcommandData data = new SubcommandData(name, description);
		data.addOptions(
				Arrays.stream(options)
						.map(SlashCommandOption::getData)
						.toList()
		);
		return data;
	}
}
