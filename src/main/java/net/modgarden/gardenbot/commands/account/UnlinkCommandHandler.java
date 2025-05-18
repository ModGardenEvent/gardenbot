package net.modgarden.gardenbot.commands.account;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.modgarden.gardenbot.GardenBot;
import net.modgarden.gardenbot.interaction.SlashCommandInteraction;
import net.modgarden.gardenbot.interaction.response.EmbedResponse;
import net.modgarden.gardenbot.interaction.response.MessageResponse;
import net.modgarden.gardenbot.interaction.response.Response;
import net.modgarden.gardenbot.util.ModGardenAPIClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.http.HttpResponse;

public class UnlinkCommandHandler {
	public static Response handleModrinthUnlink(SlashCommandInteraction interaction) {
		User user = interaction.event().getUser();
		try {
			HttpResponse<InputStream> stream = ModGardenAPIClient.get("user/" + user.getId() + "?service=discord", HttpResponse.BodyHandlers.ofInputStream());
			if (stream.statusCode() == 200) {
				JsonElement json = JsonParser.parseReader(new InputStreamReader(stream.body()));
				if (json.isJsonObject() && !json.getAsJsonObject().has("modrinth_id")) {
					return new MessageResponse()
							.setMessage("You do not have a Modrinth account linked to your Mod Garden account.")
							.markEphemeral();
				}
			} else if (stream.statusCode() == 404) {
				return new MessageResponse()
						.setMessage("You do not have a Mod Garden account.\nPlease create one with **/account create**.")
						.markEphemeral();
			}
			JsonElement json = JsonParser.parseReader(new InputStreamReader(stream.body()));
			String errorDescription = json.isJsonObject() && json.getAsJsonObject().has("description") ?
					json.getAsJsonObject().getAsJsonPrimitive("description").getAsString() :
					"Undefined Error.";
			return new EmbedResponse()
					.setTitle("Encountered an exception whilst attempting to send the setup for unlinking your Mod Garden account from Modrinth.")
					.setDescription(stream.statusCode() + ": " + errorDescription + "\nPlease report this to a team member.")
					.setColor(0xFF0000)
					.markEphemeral();
		} catch (IOException | InterruptedException ex) {
			GardenBot.LOG.error("", ex);
		}

		return new EmbedResponse()
				.setTitle("Are you sure?")
				.setDescription("Are you sure you want to unlink your current Modrinth account?")
				.setColor(0X5D3E40)
				.addButton(
						"unlinkModrinth",
						"Unlink",
						ButtonStyle.DANGER,
						Emoji.fromUnicode("U+26D3U+FE0FU+200DU+1F4A5")
				).markEphemeral();
	}
}
