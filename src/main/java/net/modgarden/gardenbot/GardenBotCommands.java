package net.modgarden.gardenbot;

import net.modgarden.gardenbot.command.account.AccountCommand;
import net.modgarden.gardenbot.command.fix.FixCommand;
import net.modgarden.gardenbot.command.submission.SubmissionCommand;
import net.modgarden.gardenbot.command.image.ImageCommand;
import net.modgarden.gardenbot.command.team.TeamCommand;

import static net.modgarden.gardenbot.interaction.dispatcher.SlashCommandDispatcher.register;

public class GardenBotCommands {

	public static void registerAll() {
		register(AccountCommand::new);
		register(FixCommand::new);
		register(ImageCommand::new);
		register(SubmissionCommand::new);
		register(TeamCommand::new);

//		SlashCommandDispatcher.register(new SlashCommand("profile", "Actions relating to your visible Mod Garden profile.",
//				new SlashCommand.SubCommandGroup(
//						"modify",
//						"Allows you to modify various options about your Mod Garden profile.",
//						new SlashCommand.SubCommand(
//								"username",
//								"Modifies your Mod Garden username.",
//								ProfileCommandHandler::handleModifyUsername,
//								new SlashCommandOption(OptionType.STRING, "username", "The new username for your account.", true, false)
//						),
//						new SlashCommand.SubCommand(
//								"displayname",
//								"Modifies your Mod Garden display name.",
//								ProfileCommandHandler::handleModifyDisplayName,
//								new SlashCommandOption(OptionType.STRING, "displayname", "The new display name for your account.", true, false)
//						),
//						new SlashCommand.SubCommand(
//								"pronouns",
//								"Modifies your pronouns on your Mod Garden profile.",
//								ProfileCommandHandler::handleModifyPronouns,
//								new SlashCommandOption(OptionType.STRING, "pronouns", "The new pronouns to show on your account.", true, false)
//						)
//				),
//				new SlashCommand.SubCommandGroup(
//						"remove",
//						"Allows you to remove various options from your Mod Garden profile.",
//						new SlashCommand.SubCommand(
//								"pronouns",
//								"Removes any pronouns from your Mod Garden profile.",
//								ProfileCommandHandler::removePronouns
//						)
//				)
//		));
	}
}
