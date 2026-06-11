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

import java.util.*;

import static net.modgarden.gardenbot.command.team.TeamCommand.getEditableProjectAutoCompleteChoices;

public class TeamKickCommand extends SlashCommand {
	public TeamKickCommand() {
		super(
				"kick",
				"Kick a user from a Mod Garden project's team.",
				new SlashCommandOption(
						OptionType.STRING,
						"project",
						"The project to kick the user from.",
						true
				),
				new SlashCommandOption(
						OptionType.USER,
						"user",
						"The user to kic from the project.",
						true
				)
		);
	}

	@NotNull
	@Override
	public Response respond(SlashCommandInteraction interaction) {
		return new MessageResponse("Kicking team members not currently implemented.");
	}


	@Override
	public List<Command.Choice> getAutoCompleteChoices(String focusedOption, User user, AutoCompletionGetter autoCompletionGetter) {
		if (focusedOption.equals("user")) {
			return Collections.emptyList();
		}

		return getEditableProjectAutoCompleteChoices(user);
	}
}
