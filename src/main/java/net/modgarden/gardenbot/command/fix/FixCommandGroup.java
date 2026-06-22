package net.modgarden.gardenbot.command.fix;

import net.modgarden.gardenbot.command.CommandGroup;
import net.modgarden.gardenbot.command.SlashCommand;

public class FixCommandGroup extends CommandGroup<SlashCommand> {
	public FixCommandGroup() {
		super(
				"fix",
				"A collection of fixes regarding your Mod Garden/Discord user data.",
				FixRolesCommand::new
		);
	}
}
