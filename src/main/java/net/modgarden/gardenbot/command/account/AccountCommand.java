package net.modgarden.gardenbot.command.account;

import net.modgarden.gardenbot.command.GroupSlashCommand;
import net.modgarden.gardenbot.command.SlashCommand;

public class AccountCommand extends GroupSlashCommand<SlashCommand> {
	public AccountCommand() {
		super(
				"account",
				"Manage your Mod Garden account.",
				new AccountCreateCommand()
		);
	}
}
