package net.modgarden.gardenbot.command.submission;

import net.modgarden.gardenbot.command.GroupSlashCommand;
import net.modgarden.gardenbot.command.SlashCommand;
import net.modgarden.gardenbot.command.submission.modrinth.SubmissionModrinthCommand;
import net.modgarden.gardenbot.command.submission.modrinth.SubmissionModrinthCreateCommand;

public class SubmissionCommand extends GroupSlashCommand<GroupSlashCommand<SlashCommand>> {
	public SubmissionCommand() {
		super(
				"submit",
				"Mod Garden event submission related actions.",
				SubmissionModrinthCommand::new
		);
	}
}
