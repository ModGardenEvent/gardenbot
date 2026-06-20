package net.modgarden.gardenbot.command.account;

import net.modgarden.gardenbot.command.CommandGroup;
import net.modgarden.gardenbot.command.SlashCommand;

public class AccountCommandGroup extends CommandGroup<SlashCommand> {
	public AccountCommandGroup() {
		super(
				"account",
				"Manage your Mod Garden account.",
				AccountCreateCommand::new
		);
	}
}
