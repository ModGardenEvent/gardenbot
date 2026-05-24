package net.modgarden.gardenbot.command.unlink;

import net.modgarden.gardenbot.command.GroupSlashCommand;
import net.modgarden.gardenbot.command.SlashCommand;

// TODO: Add functionality to Backend V2.
public class UnlinkCommand extends GroupSlashCommand<SlashCommand> {
	public UnlinkCommand() {
		super(
				"link",
				"Link different services to your Mod Garden account.",
				UnlinkMinecraftSubCommand::new,
				UnlinkModrinthSubCommand::new
		);
	}
}
