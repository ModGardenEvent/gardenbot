package net.modgarden.gardenbot.command;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.modgarden.gardenbot.client.exception.HypertextException;
import net.modgarden.gardenbot.interaction.InteractionHandler;
import net.modgarden.gardenbot.interaction.SlashCommandInteraction;
import net.modgarden.gardenbot.response.EmbedResponse;
import net.modgarden.gardenbot.response.ModalResponse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public abstract class AbstractSlashCommand implements InteractionHandler<SlashCommandInteraction> {
	public final String name;
	public final String description;
	public final SlashCommandOption[] options;

	public AbstractSlashCommand(String name, String description, SlashCommandOption... options) {
		this.name = name;
		this.description = description;
		this.options = options;
	}

	public List<Command.Choice> getAutoCompleteChoices(String focusedOption,
	                                                   User user,
	                                                   AutoCompletionGetter autoCompletionGetter,
	                                                   @Nullable String groupName,
	                                                   @Nullable String subCommandName) {
		return Collections.emptyList();
	}

	public abstract SlashCommandData asData();

	protected static EmbedResponse exceptionResponse(HypertextException exception) {
		return exceptionResponse(exception.getStatus() + ": " + exception.getMessage());
	}

	protected static EmbedResponse exceptionResponse(String message) {
		return new EmbedResponse()
				.setTitle("Encountered an exception!")
				.setDescription(message + "\nPlease report this to a team member.")
				.markEphemeral()
				.setColor(0xFF0000);
	}
}
