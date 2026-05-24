package net.modgarden.gardenbot.command.account;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.dv8tion.jda.api.entities.User;
import net.modgarden.gardenbot.GardenBot;
import net.modgarden.gardenbot.GardenBotModals;
import net.modgarden.gardenbot.command.SlashCommand;
import net.modgarden.gardenbot.interaction.SlashCommandInteraction;
import net.modgarden.gardenbot.response.EmbedResponse;
import net.modgarden.gardenbot.response.MessageResponse;
import net.modgarden.gardenbot.response.ModalResponse;
import net.modgarden.gardenbot.response.Response;
import net.modgarden.gardenbot.util.ModGardenAPIClient;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
			HttpResponse<InputStream> getUserStream = ModGardenAPIClient.get(
					"v2/users/" + user.getId() + "?by=integration_discord",
					HttpResponse.BodyHandlers.ofInputStream()
			);
			if (getUserStream.statusCode() == 200) {
				return new MessageResponse("You already have an account with Mod Garden.")
						.markEphemeral();
			}
			if (getUserStream.statusCode() != 404) {
				JsonElement json = JsonParser.parseReader(new InputStreamReader(getUserStream.body()));
				String description = json.isJsonObject() && json.getAsJsonObject().has("description")
						? json.getAsJsonObject().get("description").getAsString()
						: "Unknown Exception";
				throw new IOException(description);
			}
		} catch (IOException | InterruptedException ex) {
			return new EmbedResponse()
					.setTitle("Encountered an exception whilst attempting to invite user to your project.")
					.setDescription(ex.getMessage() + "\nPlease report this to a team member.")
					.markEphemeral()
					.setColor(0xFF0000);
		}

		return new ModalResponse(GardenBotModals.REGISTER);
	}
}
