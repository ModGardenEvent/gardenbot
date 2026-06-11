package net.modgarden.gardenbot.command.submission.modrinth;

import net.modgarden.gardenbot.command.GroupSlashCommand;
import net.modgarden.gardenbot.command.SlashCommand;

public class SubmissionModrinthCommand extends GroupSlashCommand<SlashCommand> {
	public SubmissionModrinthCommand() {
		super(
				"modrinth",
				"Submission actions relating to Modrinth as a source.",
				SubmissionModrinthCreateCommand::new
		);
	}
}
