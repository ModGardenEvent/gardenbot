package net.modgarden.gardenbot.command.event;

import net.modgarden.gardenbot.command.GroupSlashCommand;
import net.modgarden.gardenbot.command.SlashCommand;

public class EventCommand extends GroupSlashCommand<SlashCommand> {
	public EventCommand() {
		super(
				"event",
				"Commands relating to Mod Garden events.",
				EventRegisterCommand::new,
				EventUnregisterCommand::new
		);
	}
}
