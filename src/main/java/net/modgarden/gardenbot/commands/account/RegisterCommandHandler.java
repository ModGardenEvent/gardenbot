package net.modgarden.gardenbot.commands.account;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.modgarden.gardenbot.GardenBot;
import net.modgarden.gardenbot.GardenBotModals;
import net.modgarden.gardenbot.interaction.ModalInteraction;
import net.modgarden.gardenbot.interaction.SlashCommandInteraction;
import net.modgarden.gardenbot.interaction.response.EmbedResponse;
import net.modgarden.gardenbot.interaction.response.MessageResponse;
import net.modgarden.gardenbot.interaction.response.ModalResponse;
import net.modgarden.gardenbot.interaction.response.Response;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class RegisterCommandHandler {
	public static Response handleRegistration(SlashCommandInteraction interaction) {
		User user = interaction.event().getUser();

		var req = HttpRequest.newBuilder(URI.create(GardenBot.API_URL + "/user/" + user.getId() + "?service=discord"))
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

	public static Response handleModal(ModalInteraction interaction) {
		User user = interaction.event().getUser();
		String uri = GardenBot.API_URL + "/register/discord?id=" + user.getId();

		ModalMapping username = interaction.event().getValue("username");
		if (!username.getAsString().matches(GardenBot.SAFE_URL_REGEX))
			return new EmbedResponse()
					.setTitle("Failed to register Mod Garden account.")
					.setDescription("Invalid characters in username.")
					.markEphemeral();
		if (username.getAsString().length() < 3)
			return new MessageResponse()
					.setMessage("Username must be at least 3 characters long.");
		uri = uri + "&username=" + username.getAsString();

		ModalMapping displayName = interaction.event().getValue("displayName");
		if (!displayName.getAsString().matches(GardenBot.SAFE_URL_REGEX))
			return new EmbedResponse()
					.setTitle("Failed to register Mod Garden account.")
					.setDescription("Invalid characters in display name.")
					.markEphemeral();
		if (displayName.getAsString().length() < 3)
			return new EmbedResponse()
					.setTitle("Failed to register Mod Garden account.")
					.setDescription("Display Name must be at least 3 characters long.")
					.markEphemeral();
		uri = uri + "&displayname=" + displayName.getAsString();

		var req = HttpRequest.newBuilder(URI.create(uri))
				.headers("Authorization", "Basic " + GardenBot.DOTENV.get("OAUTH_SECRET"))
				.POST(HttpRequest.BodyPublishers.noBody())
				.build();
		try {
			HttpResponse<InputStream> stream = GardenBot.HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofInputStream());
			if (stream.statusCode() < 200 || stream.statusCode() > 299) {
				JsonElement json = JsonParser.parseReader(new InputStreamReader(stream.body()));
				String errorDescription = json.isJsonObject() && json.getAsJsonObject().has("description") ?
						json.getAsJsonObject().getAsJsonPrimitive("description").getAsString() :
						"Undefined Error.";
				return new EmbedResponse()
						.setTitle("Failed to register Mod Garden account.")
						.setDescription(stream.statusCode() + ": " + errorDescription)
						.markEphemeral();
			}
		} catch (IOException | InterruptedException ex) {
			GardenBot.LOG.error("", ex);
		}

		return new MessageResponse()
				.setMessage("Your Mod Garden account has successfully been registered!")
				.markEphemeral();
	}
}
