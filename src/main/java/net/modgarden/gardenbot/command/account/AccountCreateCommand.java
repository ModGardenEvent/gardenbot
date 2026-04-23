package net.modgarden.gardenbot.command.account;

import net.dv8tion.jda.api.entities.User;
import net.modgarden.gardenbot.GardenBot;
import net.modgarden.gardenbot.GardenBotModals;
import net.modgarden.gardenbot.command.SlashCommand;
import net.modgarden.gardenbot.interaction.SlashCommandInteraction;
import net.modgarden.gardenbot.response.MessageResponse;
import net.modgarden.gardenbot.response.ModalResponse;
import net.modgarden.gardenbot.response.Response;
import net.modgarden.gardenbot.util.ModGardenAPIClient;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpResponse;

public class AccountCreateCommand extends SlashCommand {
	public AccountCreateCommand() {
		super(
				"create",
				"Registers a Mod Garden account for yourself."
		);
	}

	@NotNull
	public Response respond(SlashCommandInteraction interaction) {
		User user = interaction.event().getUser();

		try {
			HttpResponse<InputStream> stream = ModGardenAPIClient.get("user/" + user.getId() + "?service=discord", HttpResponse.BodyHandlers.ofInputStream());
			if (stream.statusCode() == 200) {
				return new MessageResponse("You already have an account with Mod Garden.")
						.markEphemeral();
			}
		} catch (IOException | InterruptedException ex) {
			GardenBot.LOG.error("", ex);
		}

		return new ModalResponse(GardenBotModals.REGISTER);
	}
}
