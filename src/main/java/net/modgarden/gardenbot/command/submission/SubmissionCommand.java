package net.modgarden.gardenbot.command.submission;

import net.modgarden.gardenbot.command.GroupSlashCommand;
import net.modgarden.gardenbot.command.SlashCommand;

public class SubmissionCommand extends GroupSlashCommand<SlashCommand> {
	public SubmissionCommand() {
		super(
				"submission",
				"Mod Garden event submission related actions.",
				SubmissionSubmitCommand::new,
				SubmissionUnsubmitCommand::new
		);
	}
}
