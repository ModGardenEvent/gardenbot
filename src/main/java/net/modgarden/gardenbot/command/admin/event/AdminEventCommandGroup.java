package net.modgarden.gardenbot.command.admin.event;

import net.modgarden.gardenbot.command.CommandGroup;
import net.modgarden.gardenbot.command.SlashCommand;

public class AdminEventCommandGroup extends CommandGroup<SlashCommand> {
	public AdminEventCommandGroup() {
		super(
				"event",
				"Commands for managing events.",
				AdminEventCreateCommand::new,
				AdminModifyEventCommand::new,
				AdminListEventRolesCommand::new,
				AdminAddEventRoleCommand::new,
				AdminRemoveEventRoleCommand::new
		);
	}
}
