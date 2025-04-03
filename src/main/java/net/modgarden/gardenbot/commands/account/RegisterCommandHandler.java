package net.modgarden.gardenbot.commands.account;

import net.dv8tion.jda.api.entities.User;
import net.modgarden.gardenbot.GardenBot;
import net.modgarden.gardenbot.GardenBotModals;
import net.modgarden.gardenbot.interaction.SlashCommandInteraction;
import net.modgarden.gardenbot.interaction.response.MessageResponse;
import net.modgarden.gardenbot.interaction.response.ModalResponse;
import net.modgarden.gardenbot.interaction.response.Response;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class RegisterCommandHandler {
	public static Response handleRegistration(SlashCommandInteraction interaction) {
		User user = interaction.event().getUser();

		var req = HttpRequest.newBuilder(URI.create(GardenBot.API_URL + "user/" + user.getId() + "?service=discord"))
				.build();
		try {
			HttpResponse<InputStream> stream = GardenBot.HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofInputStream());
			if (stream.statusCode() == 200) {
				return new MessageResponse()
						.setMessage("You are already registered with Mod Garden.")
						.markEphemeral();
			}
		} catch (IOException | InterruptedException ex) {
			GardenBot.LOG.error("", ex);
		}

		return new ModalResponse(GardenBotModals.REGISTER);
	}
}
