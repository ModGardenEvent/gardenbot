package net.modgarden.gardenbot.command.unlink;

import net.modgarden.gardenbot.command.CommandGroup;
import net.modgarden.gardenbot.command.SlashCommand;

// TODO: Add functionality to Backend V2.
public class UnlinkCommandGroup extends CommandGroup<SlashCommand> {
	public UnlinkCommandGroup() {
		super(
				"link",
				"Link different services to your Mod Garden account.",
				UnlinkMinecraftSubCommand::new,
				UnlinkModrinthSubCommand::new
		);
	}
}
