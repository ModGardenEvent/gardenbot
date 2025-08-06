package net.modgarden.gardenbot;

import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.modgarden.gardenbot.commands.account.*;
import net.modgarden.gardenbot.commands.event.*;
import net.modgarden.gardenbot.interaction.command.SlashCommand;
import net.modgarden.gardenbot.interaction.command.SlashCommandDispatcher;
import net.modgarden.gardenbot.interaction.command.SlashCommandOption;

public class GardenBotCommands {

	public static void registerAll() {
		SlashCommandDispatcher.register(new SlashCommand("account", "Manage your Mod Garden account.",
				new SlashCommand.SubCommand(
						"create",
						"Registers a Mod Garden account for yourself.", CreateCommandHandler::handleAccountCreation
				)));
		SlashCommandDispatcher.register(new SlashCommand("link", "Link different services to your Mod Garden account.",
				new SlashCommand.SubCommand(
						"modrinth",
						"Provides setup to link your Modrinth account",
						LinkCommandHandler::handleModrinthLink
				),
				new SlashCommand.SubCommand(
						"minecraft",
						"Provides setup to link your Minecraft account",
						LinkCommandHandler::handleMinecraftLink
				)));
		SlashCommandDispatcher.register(new SlashCommand("unlink", "Unlink different services from your Mod Garden account.",
				new SlashCommand.SubCommand(
						"modrinth",
						"Unlinks your Modrinth account from Mod Garden",
						UnlinkCommandHandler::handleModrinthUnlink
				),
				new SlashCommand.SubCommand(
						"minecraft",
						"Unlinks your Minecraft account(s) from Mod Garden",
						UnlinkCommandHandler::handleMinecraftUnlink,
						UnlinkCommandHandler::getMinecraftChoices,
						new SlashCommandOption(OptionType.STRING, "account", "The username of the Minecraft account to unlink.", true, true)
				)));

		SlashCommandDispatcher.register(new SlashCommand("profile", "Actions relating to your visible Mod Garden profile.",
				new SlashCommand.SubCommandGroup(
						"modify",
						"Allows you to modify various options about your Mod Garden profile.",
						new SlashCommand.SubCommand(
								"username",
								"Modifies your Mod Garden username.",
								ProfileCommandHandler::handleModifyUsername,
								new SlashCommandOption(OptionType.STRING, "username", "The new username for your account.", true, false)
						),
						new SlashCommand.SubCommand(
								"displayname",
								"Modifies your Mod Garden display name.",
								ProfileCommandHandler::handleModifyDisplayName,
								new SlashCommandOption(OptionType.STRING, "displayname", "The new display name for your account.", true, false)
						),
						new SlashCommand.SubCommand(
								"pronouns",
								"Modifies your pronouns on your Mod Garden profile.",
								ProfileCommandHandler::handleModifyPronouns,
								new SlashCommandOption(OptionType.STRING, "pronouns", "The new pronouns to show on your account.", true, false)
						)
				),
				new SlashCommand.SubCommandGroup(
						"remove",
						"Allows you to remove various options from your Mod Garden profile.",
						new SlashCommand.SubCommand(
								"pronouns",
								"Removes any pronouns from your Mod Garden profile.",
								ProfileCommandHandler::removePronouns
						)
				)
		));


		SlashCommandDispatcher.register(new SlashCommand("event", "Actions relating to Mod Garden events.",
				new SlashCommand.SubCommand(
						"register",
						"Registers you to a current Mod Garden event.",
						RegisterHandler::handleEventRegister
				),
				new SlashCommand.SubCommand(
						"unregister",
						"Unregisters you from a current Mod Garden event.",
						UnregisterHandler::handleEventUnregister
				),
				new SlashCommand.SubCommand(
						"submit",
						"Submit your project to a current Mod Garden event.",
						SubmitHandler::handleSubmit,
						SubmitHandler::getChoices,
						new SlashCommandOption(OptionType.STRING, "source", "The source of your project.", true, true),
						new SlashCommandOption(OptionType.STRING, "slug", "The slug of the project to submit.", true, false)
				),
				new SlashCommand.SubCommand(
						"unsubmit",
						"Unsubmit your project from a current Mod Garden event.",
						UnsubmitHandler::handleUnsubmit,
						UnsubmitHandler::getChoices,
						new SlashCommandOption(OptionType.STRING, "slug", "The slug of the project to unsubmit.", true, true)
				),
				new SlashCommand.SubCommand(
						"update",
						"Updates your project to the latest release, or a specific release in the database.",
						UpdateHandler::handleUpdate,
						UpdateHandler::getChoices,
						new SlashCommandOption(OptionType.STRING, "source", "The source of your project.", true, true),
						new SlashCommandOption(OptionType.STRING, "slug", "The slug of the project to update.", true, true),
						new SlashCommandOption(OptionType.STRING, "version", "The version of the project to update to.", false, true)
				)));

		// TODO: Implement Ban command.
//		SlashCommandDispatcher.register(new SlashCommand("ban", "Bans a user from the Mod Garden Discord.", BanCommandHandler::handleBan,
//				new SlashCommandOption(OptionType.USER, "user", "The user to ban.", true),
//				new SlashCommandOption(OptionType.STRING, "reason", "The reason for the ban.", true),
//				new SlashCommandOption(OptionType.STRING, "duration", "The duration of the ban. (Examples: perma, permanent, 1d, 7d, 28d, 1y)", true),
//				new SlashCommandOption(OptionType.STRING, "delete_until", "The duration to delete all messages before. Has a maximum of 7 days. (Examples: 1d, 4d, 7d)", false)));
	}
}
