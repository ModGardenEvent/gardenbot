package net.modgarden.gardenbot.command.team;

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

import java.util.List;

import static net.modgarden.gardenbot.command.team.TeamCommand.getProjectAutoCompleteChoices;

public class TeamLeaveCommand extends SlashCommand {
	public TeamLeaveCommand() {
		super(
				"leave",
				"Leave a Mod Garden project.",
				new SlashCommandOption(
						OptionType.STRING,
						"project",
						"The project to leave.",
						true
				)
		);
	}

	@NotNull
	@Override
	public Response respond(SlashCommandInteraction interaction) {
		return new MessageResponse("Leaving teams is not yet implemented.");
	}


	@Override
	public List<Command.Choice> getAutoCompleteChoices(String focusedOption,
	                                                   User user,
	                                                   AutoCompletionGetter autoCompletionGetter) {
		return getProjectAutoCompleteChoices(user);
	}
}
