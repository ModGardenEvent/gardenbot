package net.modgarden.gardenbot.command.admin;

import net.modgarden.gardenbot.command.CommandGroup;
import net.modgarden.gardenbot.command.SlashCommand;
import net.modgarden.gardenbot.command.admin.event.AdminEventCommandGroup;
import net.modgarden.gardenbot.command.admin.role.AdminRoleCommandGroup;

public class AdminCommandGroup extends CommandGroup<CommandGroup<SlashCommand>> {
	public AdminCommandGroup() {
		super(
				"admin",
				"Actions for managing the Garden. (requires sudo)",
				AdminEventCommandGroup::new,
				AdminRoleCommandGroup::new
		);
	}
}
