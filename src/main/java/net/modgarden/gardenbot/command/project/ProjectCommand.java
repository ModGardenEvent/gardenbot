package net.modgarden.gardenbot.command.project;

import net.modgarden.gardenbot.command.GroupSlashCommand;
import net.modgarden.gardenbot.command.SlashCommand;
import net.modgarden.gardenbot.command.submission.modrinth.SubmissionModrinthCommand;

public class ProjectCommand extends GroupSlashCommand<SlashCommand> {
	public ProjectCommand() {
		super(
				"project",
				"Mod Garden project related actions.",
				ProjectCreateCommand::new
		);
	}
}
