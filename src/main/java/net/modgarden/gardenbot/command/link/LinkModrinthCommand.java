package net.modgarden.gardenbot.command.link;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.modgarden.gardenbot.GardenBot;
import net.modgarden.gardenbot.command.SlashCommand;
import net.modgarden.gardenbot.interaction.SlashCommandInteraction;
import net.modgarden.gardenbot.response.EmbedResponse;
import net.modgarden.gardenbot.response.MessageResponse;
import net.modgarden.gardenbot.response.Response;
import net.modgarden.gardenbot.util.ModGardenAPIClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpResponse;

// TODO: Rewrite for Backend V2.
public class LinkModrinthCommand extends SlashCommand {
	public LinkModrinthCommand() {
		super(
				"modrinth",
				"Provides setup to link a Modrinth account"
		);
	}

	@NotNull
	@Override
	public Response respond(SlashCommandInteraction interaction) {
		User user = interaction.event().getUser();

		Response error = handleErrorResponse(user);

		if (error != null) {
			return error;
		}

		return new EmbedResponse()
				.setTitle("Link your Modrinth Account!")
				.setDescription(
						"""
						1. Authorize with Modrinth, which will redirect you to a page with a link code.
						2. 2. Enter your link code inside the modal.

						You may only have one Modrinth account linked to your Mod Garden account.
						""")
				.setColor(0xA9FFA7)
				.addButtonUrl(
						URI.create("""
							https://modrinth.com/auth/authorize
								?client_id=Q2tuKyb4
								&redirect_uri=%s
								&scope=USER_READ+PROJECT_READ+VERSION_READ+ORGANIZATION_READ"""
								.formatted(GardenBot.API_URL + "/discord/oauth/modrinth")
						),
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

	@Nullable
	private static Response handleErrorResponse(User user) {
		try {
			HttpResponse<InputStream> stream = ModGardenAPIClient.get("user/" + user.getId() + "?service=discord", HttpResponse.BodyHandlers.ofInputStream());
			if (stream.statusCode() == 200) {
				JsonElement json = JsonParser.parseReader(new InputStreamReader(stream.body()));
				if (json.isJsonObject() && json.getAsJsonObject().has("modrinth_id")) {
					return new MessageResponse("You already have a Modrinth account linked!\nRun **/unlink modrinth** to unlink your current account then try again.")
							.markEphemeral();
				}
			}

			if (stream.statusCode() == 404) {
				return new MessageResponse("You do not have a Mod Garden account.\nPlease create one with **/account create**.")
						.markEphemeral();
			}

			if (stream.statusCode() < 200 && stream.statusCode() > 299) {
				JsonElement json = JsonParser.parseReader(new InputStreamReader(stream.body()));
				String errorDescription =
						json.isJsonObject() && json.getAsJsonObject().has("description")
								? json.getAsJsonObject().getAsJsonPrimitive("description").getAsString()
								: "Undefined Error.";
				return new EmbedResponse()
						.setTitle("Encountered an exception whilst attempting to send the setup for linking your Modrinth account to your Mod Garden account.")
						.setDescription(stream.statusCode() + ": " + errorDescription + "\nPlease report this to a team member.")
						.setColor(0xFF0000)
						.markEphemeral();
			}
		} catch (IOException | InterruptedException ex) {
			GardenBot.LOG.error("", ex);
		}

		return null;
	}
}
