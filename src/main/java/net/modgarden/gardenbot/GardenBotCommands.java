package net.modgarden.gardenbot;

import net.modgarden.gardenbot.command.account.*;
import net.modgarden.gardenbot.command.link.LinkCommand;
import net.modgarden.gardenbot.command.submit.SubmitCommand;
import net.modgarden.gardenbot.command.unlink.UnlinkCommand;
import net.modgarden.gardenbot.command.unsubmit.UnsubmitCommand;

import static net.modgarden.gardenbot.interaction.dispatcher.SlashCommandDispatcher.register;

public class GardenBotCommands {

	public static void registerAll() {
		register(new AccountCommand());
		register(new LinkCommand());
		register(new UnlinkCommand());
		register(new SubmitCommand());
		register(new UnsubmitCommand());


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
//
//
//		SlashCommandDispatcher.register(new SlashCommand("event", "Actions relating to Mod Garden events.",
//				new SlashCommand.SubCommand(
//						"register",
//						"Registers you to a current Mod Garden event.",
//						RegisterHandler::handleEventRegister
//				),
//				new SlashCommand.SubCommand(
//						"unregister",
//						"Unregisters you from a current Mod Garden event.",
//						UnregisterHandler::handleEventUnregister
//				),
//				new SlashCommand.SubCommand(
//						"update",
//						"Updates your project to the latest release, or a specific release in the database.",
//						UpdateHandler::handleUpdate,
//						UpdateHandler::getChoices,
//						new SlashCommandOption(OptionType.STRING, "source", "The source of your project.", true, true),
//						new SlashCommandOption(OptionType.STRING, "project", "The project to update.", true, true),
//						new SlashCommandOption(OptionType.STRING, "version", "The version of the project to update to.", false, true)
//				));
//
//		SlashCommandDispatcher.register(new SlashCommand("team", "Actions relating to your Mod Garden projects.",
//				new SlashCommand.SubCommand(
//						"invite",
//						"Invites a user to your Mod Garden project.",
//						InviteHandler::handleInvite,
//						InviteHandler::getChoices,
//						new SlashCommandOption(OptionType.STRING, "project", "The project to invite the user to.", true, true),
//						new SlashCommandOption(OptionType.STRING, "role", "The role to provide the user.", true, true),
//						new SlashCommandOption(OptionType.USER, "user", "The user to invite.", true, false)
//				),
//				new SlashCommand.SubCommand(
//						"leave",
//						"Leaves a Mod Garden project.",
//						LeaveHandler::handleLeave,
//						LeaveHandler::getChoices,
//						new SlashCommandOption(OptionType.STRING, "project", "The project to leave.", true, true)
//				),
//				new SlashCommand.SubCommand(
//						"kick",
//						"Kicks a user from a Mod Garden project.",
//						KickHandler::handleKick,
//						KickHandler::getChoices,
//						new SlashCommandOption(OptionType.STRING, "project", "The project to kick the user from.", true, true),
//						new SlashCommandOption(OptionType.USER, "user", "The user to kick.", true, false)
//				)
//		));
//
//		SlashCommandDispatcher.register(new SlashCommand("image", "Actions relating to images for Mod Garden's showcase worlds.",
//				new SlashCommand.SubCommand(
//						"upload",
//						"Uploads an image to the public Mod Garden CDN.",
//						UploadHandler::handleUpload,
//						new SlashCommandOption(OptionType.ATTACHMENT, "attachment", "A PNG image to upload.", true)
//				)
//		));

		// TODO: Implement Ban command.
//		SlashCommandDispatcher.register(new SlashCommand("ban", "Bans a user from the Mod Garden Discord.", BanCommand::handleBan,
//				new SlashCommandOption(OptionType.USER, "user", "The user to ban.", true),
//				new SlashCommandOption(OptionType.STRING, "reason", "The reason for the ban.", true),
//				new SlashCommandOption(OptionType.STRING, "duration", "The duration of the ban. (Examples: perma, permanent, 1d, 7d, 28d, 1y)", true),
//				new SlashCommandOption(OptionType.STRING, "delete_until", "The duration to delete all messages before. Has a maximum of 7 days. (Examples: 1d, 4d, 7d)", false)));
	}
}
