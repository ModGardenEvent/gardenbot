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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class LinkCommandHandler {
	public static Response handleModrinthLink(SlashCommandInteraction interaction) {
		User user = interaction.event().getUser();

		var req = HttpRequest.newBuilder(URI.create(GardenBot.API_URL + "user/" + user.getId() + "?service=discord"))
				.build();
		try {
			HttpResponse<InputStream> stream = GardenBot.HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofInputStream());
			if (stream.statusCode() == 200) {
				JsonElement json = JsonParser.parseReader(new InputStreamReader(stream.body()));
				if (json.isJsonObject() && json.getAsJsonObject().has("modrinth_id"))
					return new MessageResponse()
							.setMessage("You already have a Modrinth account linked!\nRun **/unlink modrinth** to unlink your current account then try again.")
							.markEphemeral();
			} else if (stream.statusCode() == 404) {
				return new MessageResponse()
						.setMessage("You do not have a Mod Garden account.\nPlease register with **/register**.");
			} else {
				JsonElement json = JsonParser.parseReader(new InputStreamReader(stream.body()));
				String errorDescription = json.isJsonObject() && json.getAsJsonObject().has("description") ?
						json.getAsJsonObject().getAsJsonPrimitive("description").getAsString() :
						"Undefined Error.";
				return new EmbedResponse()
						.setTitle("Encountered an exception whilst attempting to send the setup for linking your Mod Garden account to Modrinth.")
						.setDescription(stream.statusCode() + ": " + errorDescription + "\nPlease report this to a team member.")
						.setColor(0xFF0000)
						.markEphemeral();
			}
		} catch (IOException | InterruptedException ex) {
			GardenBot.LOG.error("", ex);
		}

		return new EmbedResponse()
				.setTitle("Link your Modrinth Account!")
				.setDescription(
						"1. Authorize with Modrinth, which will redirect you to a page with a link code.\n" +
						"2. Enter your link code inside the modal.")
				.setColor(0xA9FFA7)
				.addButtonUrl(
						URI.create("https://modrinth.com/auth/authorize?client_id=4g0H4NkM&redirect_uri=" + GardenBot.API_URL + "discord/oauth/modrinth&scope=USER_READ+PROJECT_READ+VERSION_READ+ORGANIZATION_READ"),
						"1. Authorize",
						Emoji.fromCustom("modrinth", 1330663190626828479L, false)
				)
				.addButton(
						"linkModrinth",
						"2. Link",
						ButtonStyle.SECONDARY,
						Emoji.fromUnicode("U+1F517")
				).markEphemeral();
	}
}
