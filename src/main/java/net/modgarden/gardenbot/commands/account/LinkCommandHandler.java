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
import java.net.URI;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class LinkCommandHandler {
	public static Response handleModrinthLink(SlashCommandInteraction interaction) {
		User user = interaction.event().getUser();

		try {
			HttpResponse<InputStream> stream = ModGardenAPIClient.get("user/" + user.getId() + "?service=discord", HttpResponse.BodyHandlers.ofInputStream());
			if (stream.statusCode() == 200) {
				JsonElement json = JsonParser.parseReader(new InputStreamReader(stream.body()));
				if (json.isJsonObject() && json.getAsJsonObject().has("modrinth_id"))
					return new MessageResponse()
							.setMessage("You already have a Modrinth account linked!\nRun **/unlink modrinth** to unlink your current account then try again.")
							.markEphemeral();
			} else if (stream.statusCode() == 404) {
				return new MessageResponse()
						.setMessage("You do not have a Mod Garden account.\nPlease create one with **/account create**.")
						.markEphemeral();
			} else if (stream.statusCode() < 200 && stream.statusCode() > 299) {
				JsonElement json = JsonParser.parseReader(new InputStreamReader(stream.body()));
				String errorDescription = json.isJsonObject() && json.getAsJsonObject().has("description") ?
						json.getAsJsonObject().getAsJsonPrimitive("description").getAsString() :
						"Undefined Error.";
				return new EmbedResponse()
						.setTitle("Encountered an exception whilst attempting to send the setup for linking your Modrinth account to your Mod Garden account.")
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
						"""
						1. Authorize with Modrinth, which will redirect you to a page with a link code.
						2. 2. Enter your link code inside the modal.

						You may only have one Modrinth account linked to your Mod Garden account.
						""")
				.setColor(0xA9FFA7)
				.addButtonUrl(
						URI.create("https://modrinth.com/auth/authorize?client_id=Q2tuKyb4&redirect_uri=" + GardenBot.API_URL + "discord/oauth/modrinth&scope=USER_READ+PROJECT_READ+VERSION_READ+ORGANIZATION_READ"),
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


	public static Response handleMinecraftLink(SlashCommandInteraction interaction) {
		interaction.event().deferReply(true).queue();
		User user = interaction.event().getUser();
		String challengeCode = null;

		try {
			HttpResponse<InputStream> userResponse = ModGardenAPIClient.get("user/" + user.getId() + "?service=discord", HttpResponse.BodyHandlers.ofInputStream());
			if (userResponse.statusCode() == 404) {
				return new MessageResponse()
						.setMessage("You do not have a Mod Garden account.\nPlease create one with **/account create**.");
			} else if (userResponse.statusCode() != 200) {
				try (var userStream = new InputStreamReader(userResponse.body())) {
					JsonElement json = JsonParser.parseReader(userStream);
					String errorDescription = json.isJsonObject() && json.getAsJsonObject().has("description") ?
							json.getAsJsonObject().getAsJsonPrimitive("description").getAsString() :
							"Undefined Error.";
					return new EmbedResponse()
							.setTitle("Encountered an exception whilst attempting to send the setup for linking your Minecraft account to your Mod Garden account.")
							.setDescription(userResponse.statusCode() + ": " + errorDescription + "\nPlease report this to a team member.")
							.setColor(0xFF0000)
							.markEphemeral();
				}
			}

			HttpResponse<InputStream> challengeCodeResponse = ModGardenAPIClient.get("discord/oauth/minecraft/challenge", HttpResponse.BodyHandlers.ofInputStream());
			try (var challengeCodeStream = new InputStreamReader(challengeCodeResponse.body(), StandardCharsets.UTF_8)) {
				challengeCode = new Scanner(challengeCodeStream).next();
			}
		} catch (IOException | InterruptedException ex) {
			GardenBot.LOG.error("", ex);
		}

		if (challengeCode == null) {
			return new EmbedResponse()
					.setTitle("Encountered an exception whilst attempting to send the setup for linking your Minecraft account to your Mod Garden account.")
					.setDescription("Failed to create challenge code for Microsoft Authentication.\nPlease report this to a team member.")
					.setColor(0xFF0000)
					.markEphemeral();
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
						URI.create("https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize?client_id=e7ee42f6-e542-4ce6-9f7b-1d31941e84c6&response_type=code&redirect_uri=" + GardenBot.API_URL.replaceAll(":", "%3A").replaceAll("/", "%2F") + "discord%2Foauth%2Fminecraft&response_mode=query&scope=XboxLive.signIn&state=" + challengeCode + "&prompt=select_account&code_challenge=" + challengeCode + "&code_challenge_method=S256"),
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
}
