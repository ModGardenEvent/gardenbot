package net.modgarden.gardenbot.command.link;

import net.modgarden.gardenbot.command.GroupSlashCommand;
import net.modgarden.gardenbot.command.SlashCommand;

public class LinkCommand extends GroupSlashCommand<SlashCommand> {
	public LinkCommand() {
		super(
				"link",
				"Link different services to your Mod Garden account.",
				new LinkMinecraftCommand(),
				new LinkModrinthCommand()
		);
	}
}
