package net.modgarden.gardenbot.command.submit;

import net.modgarden.gardenbot.command.GroupSlashCommand;
import net.modgarden.gardenbot.command.SlashCommand;

public class SubmitCommand extends GroupSlashCommand<SlashCommand> {
	public SubmitCommand() {
		super(
				"submit",
				"Submit your project to a current Mod Garden event.",
				new SubmitModrinthCommand()
		);
	}
}
