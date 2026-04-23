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
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

// TODO: Rewrite for Backend V2.
public class LinkMinecraftCommand extends SlashCommand {
	public LinkMinecraftCommand() {
		super(
				"minecraft",
				"Provides setup to link a Minecraft account"
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

		String challengeCode = null;

		try {
			HttpResponse<InputStream> challengeCodeResponse = ModGardenAPIClient.get("discord/oauth/minecraft/challenge", HttpResponse.BodyHandlers.ofInputStream());
			try (var challengeCodeStream = new InputStreamReader(challengeCodeResponse.body(), StandardCharsets.UTF_8)) {
				challengeCode = new Scanner(challengeCodeStream).next();
			}

			if (challengeCode == null) {
				return new EmbedResponse()
						.setTitle("Encountered an exception whilst attempting to send the setup for linking your Minecraft account to your Mod Garden account.")
						.setDescription("Failed to create challenge code for Microsoft Authentication.\nPlease report this to a team member.")
						.setColor(0xFF0000)
						.markEphemeral();
			}
		} catch (IOException | InterruptedException ex) {
			GardenBot.LOG.error("", ex);
		}

		return new EmbedResponse()
				.setTitle("Link your Minecraft Account!")
				.setDescription(
						"""
						1. Authorize with Microsoft, which will redirect you to a page with a link code.
						2. Enter your link code inside the modal.

						You may have multiple Minecraft accounts linked to your Mod Garden account.
						You are only able to use each Microsoft Authorization link once. Please generate a new message if you need another one.
						""")
				.setColor(0xA9FFA7)
				.addButtonUrl(
						URI.create("""
								https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize
								?client_id=e7ee42f6-e542-4ce6-9f7b-1d31941e84c6
									&response_type=code
									&redirect_uri=%s
									&response_mode=query
									&scope=XboxLive.signIn
									&state=challengeCode
									&prompt=select_account
									&code_challenge=%s
									&code_challenge_method=S256
								""".formatted(sanitizeRedirectUrl(GardenBot.API_URL + "discord/oauth/minecraft"), challengeCode)),
						"1. Authorize",
						Emoji.fromCustom("microsoft", 1360176270687731842L, false)
				)
				.addButton(
						"linkMinecraft",
						"2. Link",
						ButtonStyle.SECONDARY,
						Emoji.fromUnicode("U+1F517")
				).markEphemeral();
	}

	@Nullable
	private static Response handleErrorResponse(User user) {
		try {
			HttpResponse<InputStream> userResponse = ModGardenAPIClient.get("user/" + user.getId() + "?service=discord", HttpResponse.BodyHandlers.ofInputStream());
			if (userResponse.statusCode() == 404) {
				return new MessageResponse("You do not have a Mod Garden account.\nPlease create one with **/account create**.");
			}

			if (userResponse.statusCode() != 200) {
				try (var userStream = new InputStreamReader(userResponse.body())) {
					JsonElement json = JsonParser.parseReader(userStream);
					String errorDescription = json.isJsonObject() && json.getAsJsonObject().has("description")
							? json.getAsJsonObject().getAsJsonPrimitive("description").getAsString()
							: "Undefined Error.";
					return new EmbedResponse()
							.setTitle("Encountered an exception whilst attempting to send the setup for linking your Minecraft account to your Mod Garden account.")
							.setDescription(userResponse.statusCode() + ": " + errorDescription + "\nPlease report this to a team member.")
							.setColor(0xFF0000)
							.markEphemeral();
				}
			}
		} catch (IOException | InterruptedException ex) {
			GardenBot.LOG.error("", ex);
		}

		return null;
	}

	private static String sanitizeRedirectUrl(String url) {
		return url.replace(":", "%3A").replace("/", "%2F");
	}
}
