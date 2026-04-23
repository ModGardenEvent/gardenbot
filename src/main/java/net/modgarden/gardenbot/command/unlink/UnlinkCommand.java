package net.modgarden.gardenbot.command.unlink;

import net.modgarden.gardenbot.command.GroupSlashCommand;
import net.modgarden.gardenbot.command.SlashCommand;

public class UnlinkCommand extends GroupSlashCommand<SlashCommand> {
	public UnlinkCommand() {
		super(
				"link",
				"Link different services to your Mod Garden account.",
				new UnlinkMinecraftSubCommand(),
				new UnlinkModrinthSubCommand()
		);
	}
}
