package net.modgarden.gardenbot.command.team;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.modgarden.gardenbot.command.AutoCompletionGetter;
import net.modgarden.gardenbot.command.GroupSlashCommand;
import net.modgarden.gardenbot.command.SlashCommand;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TeamCommand extends GroupSlashCommand<SlashCommand> {
	public TeamCommand() {
		super(
			"team",
			"Modify the team of a Mod Garden project.",
				InviteCommand::new,
				KickCommand::new,
				LeaveCommand::new
		);
	}

	public static List<Command.Choice> getProjectAutoCompleteChoices(User user, AutoCompletionGetter autoCompletionGetter) {
		// TODO: Handle Project Lookup.
		return Collections.emptyList();
	}
}
