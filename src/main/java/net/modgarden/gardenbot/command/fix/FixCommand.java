package net.modgarden.gardenbot.command.fix;

import net.modgarden.gardenbot.command.GroupSlashCommand;
import net.modgarden.gardenbot.command.SlashCommand;

public class FixCommand extends GroupSlashCommand<SlashCommand> {
	public FixCommand() {
		super(
				"fix",
				"A collection of fixes regarding your Mod Garden/Discord user data.",
				FixRolesCommand::new
		);
	}
}
