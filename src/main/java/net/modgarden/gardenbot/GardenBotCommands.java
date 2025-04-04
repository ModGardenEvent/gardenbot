package net.modgarden.gardenbot;

import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.modgarden.gardenbot.commands.account.LinkCommandHandler;
import net.modgarden.gardenbot.commands.account.RegisterCommandHandler;
import net.modgarden.gardenbot.commands.account.UnlinkCommandHandler;
import net.modgarden.gardenbot.commands.moderation.BanCommandHandler;
import net.modgarden.gardenbot.interaction.command.SlashCommand;
import net.modgarden.gardenbot.interaction.command.SlashCommandDispatcher;
import net.modgarden.gardenbot.interaction.command.SlashCommandOption;

public class GardenBotCommands {

	public static void registerAll() {
		SlashCommandDispatcher.register(new SlashCommand("register", "Registers a Mod Garden account for yourself.", RegisterCommandHandler::handleRegistration));
		SlashCommandDispatcher.register(new SlashCommand("link", "Link your account with different services.",
				new SlashCommand.SubCommand(
						"modrinth",
						"Provides setup to link your account with Modrinth",
						LinkCommandHandler::handleModrinthLink
				)));
		SlashCommandDispatcher.register(new SlashCommand("unlink", "Unlink your account from different services.",
				new SlashCommand.SubCommand(
						"modrinth",
						"Unlinks your account from Modrinth",
						UnlinkCommandHandler::handleModrinthUnlink
				)));

		SlashCommandDispatcher.register(new SlashCommand("ban", "Bans a user from the Mod Garden Discord.", BanCommandHandler::handleBan,
				new SlashCommandOption(OptionType.USER, "user", "The user to ban.", true),
				new SlashCommandOption(OptionType.STRING, "reason", "The reason for the ban.", true),
				new SlashCommandOption(OptionType.STRING, "duration", "The duration of the ban. (Examples: perma, permanent, 1d, 7d, 28d, 1y)", true),
				new SlashCommandOption(OptionType.STRING, "delete_until", "The duration to delete all messages before. Has a maximum of 7 days. (Examples: 1d, 4d, 7d)", false)));
	}
}
