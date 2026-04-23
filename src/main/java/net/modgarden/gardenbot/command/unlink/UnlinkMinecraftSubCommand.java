package net.modgarden.gardenbot.command.unlink;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.modgarden.gardenbot.GardenBot;
import net.modgarden.gardenbot.command.AutoCompletionGetter;
import net.modgarden.gardenbot.command.SlashCommand;
import net.modgarden.gardenbot.command.SlashCommandOption;
import net.modgarden.gardenbot.interaction.SlashCommandInteraction;
import net.modgarden.gardenbot.response.EmbedResponse;
import net.modgarden.gardenbot.response.MessageResponse;
import net.modgarden.gardenbot.response.Response;
import net.modgarden.gardenbot.util.MinecraftAccountUtil;
import net.modgarden.gardenbot.util.ModGardenAPIClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.http.HttpResponse;
import java.util.*;

// TODO: Rewrite for Backend V2.
public class UnlinkMinecraftSubCommand extends SlashCommand {
	public UnlinkMinecraftSubCommand() {
		super(
				"minecraft",
				"Unlinks a Minecraft account from Mod Garden",
				new SlashCommandOption(
						OptionType.STRING,
						"account",
						"The username of the Minecraft account to unlink.",
						true,
						true
				)
		);
	}

	@NotNull
	@Override
	public Response respond(SlashCommandInteraction interaction) {
		User user = interaction.event().getUser();

		String account = interaction.event().getOption("account", OptionMapping::getAsString);
		String uuid = MinecraftAccountUtil.getUuidFromMinecraftUsername(account);
		if (uuid == null) {
			uuid = account;
		}

		Response error = handleErrorResponse(user, uuid);
		if (error != null) {
			return error;
		}

		return new EmbedResponse()
				.setTitle("Are you sure?")
				.setDescription("Are you sure you want to unlink your Minecraft account?")
				.setColor(0X5D3E40)
				.addButton(
						"unlinkMinecraft?" + uuid,
						"Unlink",
						ButtonStyle.DANGER,
						Emoji.fromUnicode("U+26D3U+FE0FU+200DU+1F4A5")
				).markEphemeral();
	}

	@Nullable
	private static Response handleErrorResponse(User user, String uuidString) {
		try {
			HttpResponse<InputStream> stream = ModGardenAPIClient.get("user/" + user.getId() + "?service=discord", HttpResponse.BodyHandlers.ofInputStream());
			if (stream.statusCode() == 200) {
				JsonElement json = JsonParser.parseReader(new InputStreamReader(stream.body()));
				if (json.isJsonObject()) {
					JsonArray minecraftAccounts = json.getAsJsonObject().getAsJsonArray("minecraft_accounts");
					if (minecraftAccounts == null || minecraftAccounts.isEmpty())
						return new MessageResponse("You do not have a Minecraft account linked to your Mod Garden account.")
								.markEphemeral();

					if (minecraftAccounts.asList()
							.stream()
							.noneMatch(jsonElement ->
									jsonElement.isJsonPrimitive() && jsonElement.getAsJsonPrimitive().isString()
											&& jsonElement.getAsJsonPrimitive().getAsString().equals(uuidString)
							)
					)
						return new MessageResponse("The specified Minecraft account is not linked to your Mod Garden account.")
								.markEphemeral();
				}
			}

			if (stream.statusCode() == 404) {
				return new MessageResponse("You do not have a Mod Garden account.\nPlease create one with **/account create**.")
						.markEphemeral();
			}

			if (stream.statusCode() < 200 && stream.statusCode() > 299) {
				JsonElement json = JsonParser.parseReader(new InputStreamReader(stream.body()));
				String errorDescription = json.isJsonObject() && json.getAsJsonObject().has("description") ?
						json.getAsJsonObject().getAsJsonPrimitive("description").getAsString() :
						"Undefined Error.";
				return new EmbedResponse()
						.setTitle("Encountered an exception whilst attempting to send the setup for unlinking your Minecraft account from Modrinth.")
						.setDescription(stream.statusCode() + ": " + errorDescription + "\nPlease report this to a team member.")
						.setColor(0xFF0000)
						.markEphemeral();
			}
		} catch (IOException | InterruptedException ex) {
			GardenBot.LOG.error("", ex);
		}

		return null;
	}


	@Override
	public List<Command.Choice> getAutoCompleteChoices(String focusedOption,
	                                                   User user,
	                                                   AutoCompletionGetter autoCompletionGetter) {
		try {
			var userResult = ModGardenAPIClient.get("user/" + user.getId() + "?service=discord", HttpResponse.BodyHandlers.ofInputStream());
			if (userResult.statusCode() != 200) {
				return Collections.emptyList();
			}
			try (InputStreamReader userReader = new InputStreamReader(userResult.body())) {
				JsonElement userJson = JsonParser.parseReader(userReader);
				if (!userJson.isJsonObject()) {
					return Collections.emptyList();
				}
				JsonArray minecraftAccounts = userJson.getAsJsonObject().getAsJsonArray("minecraft_accounts");
				if (minecraftAccounts != null) {
					return minecraftAccounts
							.getAsJsonArray()
							.asList()
							.parallelStream()
							.map(accountJson -> {
								if (!accountJson.isJsonPrimitive() || !accountJson.getAsJsonPrimitive().isString()) {
									return null;
								}
								String uuid = accountJson.getAsString();
								String username = MinecraftAccountUtil.getMinecraftUsernameFromUuid(uuid);
								if (username != null) {
									return new Command.Choice(username, username);
								}
								return new Command.Choice(uuid, uuid);
							}).filter(Objects::nonNull).toList();
				}
			}
		} catch (IOException | InterruptedException ex) {
			GardenBot.LOG.error("Could not get Minecraft accounts from user.", ex);
		}
		return Collections.emptyList();
	}
}
