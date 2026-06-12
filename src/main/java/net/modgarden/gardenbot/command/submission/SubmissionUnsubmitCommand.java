package net.modgarden.gardenbot.command.submission;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.modgarden.gardenbot.command.AutoCompletionGetter;
import net.modgarden.gardenbot.command.SlashCommand;
import net.modgarden.gardenbot.command.SlashCommandOption;
import net.modgarden.gardenbot.interaction.SlashCommandInteraction;
import net.modgarden.gardenbot.response.MessageResponse;
import net.modgarden.gardenbot.response.Response;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

// TODO: Backend V2.
public class SubmissionUnsubmitCommand extends SlashCommand {
	public SubmissionUnsubmitCommand() {
		super(
				"unsubmit",
				"Unsubmit a submission from a current Mod Garden event.",
				new SlashCommandOption(
						OptionType.STRING,
						"submission",
						"The submission to unsubmit.",
						true,
						true
				)
		);
	}

	@NotNull
	@Override
	public Response respond(SlashCommandInteraction interaction) {
		interaction.event().deferReply(true).queue();
		User user = interaction.event().getUser();

		return new MessageResponse("Unsubmit command is not yet implemented");
	}


	public List<Command.Choice> getAutoCompleteChoices(String focusedOption,
	                                                   User user,
	                                                   AutoCompletionGetter optionCompletionGetter) {
		return Collections.emptyList();
	}
}
