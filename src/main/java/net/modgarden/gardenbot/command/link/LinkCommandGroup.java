package net.modgarden.gardenbot.command.link;

import net.modgarden.gardenbot.command.CommandGroup;
import net.modgarden.gardenbot.command.SlashCommand;

public class LinkCommandGroup extends CommandGroup<SlashCommand> {
	public LinkCommandGroup() {
		super(
				"link",
				"Link different services to your Mod Garden account.",
				LinkModrinthCommand::new
		);
	}
}
