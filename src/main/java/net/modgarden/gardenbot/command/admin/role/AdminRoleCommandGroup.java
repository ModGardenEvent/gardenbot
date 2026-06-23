package net.modgarden.gardenbot.command.admin.role;

import net.modgarden.gardenbot.command.CommandGroup;
import net.modgarden.gardenbot.command.SlashCommand;

public class AdminRoleCommandGroup extends CommandGroup<SlashCommand> {
	public AdminRoleCommandGroup() {
		super(
				"role",
				"Commands for managing roles.",
				AdminRoleCreateCommand::new,
				AdminRoleCopyCommand::new,
				AdminRoleDeleteCommand::new,
				AdminRoleModifyCommand::new
		);
	}
}
