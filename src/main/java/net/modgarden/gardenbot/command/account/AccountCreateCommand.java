package net.modgarden.gardenbot.command.account;

import net.dv8tion.jda.api.entities.User;
import net.modgarden.gardenbot.GardenBot;
import net.modgarden.gardenbot.GardenBotModals;
import net.modgarden.gardenbot.client.ModGarden;
import net.modgarden.gardenbot.client.mod_garden.user.ModGardenUser;
import net.modgarden.gardenbot.command.SlashCommand;
import net.modgarden.gardenbot.interaction.SlashCommandInteraction;
import net.modgarden.gardenbot.response.MessageResponse;
import net.modgarden.gardenbot.response.ModalResponse;
import net.modgarden.gardenbot.response.Response;
import org.jetbrains.annotations.NotNull;

public class AccountCreateCommand extends SlashCommand {
	public AccountCreateCommand() {
		super(
				"create",
				"Registers a Mod Garden account for yourself."
		);
	}

	// TODO: Figure out a way to not time out Discord. Modals do not work with deferReply...
	@NotNull
	public Response respond(SlashCommandInteraction interaction) {
		User user = interaction.event().getUser();

		try {
			ModGardenUser mgUser = ModGarden.getUserByDiscordUser(user);
			if (mgUser != null) {
				return new MessageResponse("You already have an account with Mod Garden.")
						.markEphemeral();
			}
		} catch (Exception e) {
			GardenBot.LOG.error("Exception whilst attempting to create account. ", e);
			return exceptionResponse(e.getMessage());
		}
		return new ModalResponse(GardenBotModals.REGISTER);
	}
}
