package net.modgarden.gardenbot;

import net.modgarden.gardenbot.command.account.AccountCommandGroup;
import net.modgarden.gardenbot.command.fix.FixCommandGroup;
import net.modgarden.gardenbot.command.image.ImageCommandGroup;
import net.modgarden.gardenbot.command.submission.SubmissionCommandGroup;
import net.modgarden.gardenbot.command.team.TeamCommandGroup;

import static net.modgarden.gardenbot.interaction.dispatcher.SlashCommandDispatcher.register;

public class GardenBotCommands {

	public static void registerAll() {
		register(AccountCommandGroup::new);
		register(FixCommandGroup::new);
		register(ImageCommandGroup::new);
		register(SubmissionCommandGroup::new);
		register(TeamCommandGroup::new);

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
