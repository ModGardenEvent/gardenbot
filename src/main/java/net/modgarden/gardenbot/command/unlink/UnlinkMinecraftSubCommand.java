package net.modgarden.gardenbot.command.unlink;

import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.modgarden.gardenbot.command.SlashCommand;
import net.modgarden.gardenbot.command.SlashCommandOption;
import net.modgarden.gardenbot.interaction.SlashCommandInteraction;
import net.modgarden.gardenbot.response.MessageResponse;
import net.modgarden.gardenbot.response.Response;
import org.jetbrains.annotations.NotNull;

// TODO: Add functionality to Backend V2.
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
		return new MessageResponse("Account integrations are not yet implemented.");

//		return new EmbedResponse()
//				.setTitle("Are you sure?")
//				.setDescription("Are you sure you want to unlink your Minecraft account?")
//				.setColor(0X5D3E40)
//				.addButton(
//						"unlinkMinecraft?" + uuid,
//						"Unlink",
//						ButtonStyle.DANGER,
//						Emoji.fromUnicode("U+26D3U+FE0FU+200DU+1F4A5")
//				).markEphemeral();
	}
//
//
//	@Override
//	public List<Command.Choice> getAutoCompleteChoices(String focusedOption,
//	                                                   User user,
//	                                                   AutoCompletionGetter autoCompletionGetter) {
//		try {
//			var userResult = ModGarden.get("user/" + user.getId() + "?service=discord", HttpResponse.BodyHandlers.ofInputStream());
//			if (userResult.statusCode() != 200) {
//				return Collections.emptyList();
//			}
//			try (InputStreamReader userReader = new InputStreamReader(userResult.body())) {
//				JsonElement userJson = JsonParser.parseReader(userReader);
//				if (!userJson.isJsonObject()) {
//					return Collections.emptyList();
//				}
//				JsonArray minecraftAccounts = userJson.getAsJsonObject().getAsJsonArray("minecraft_accounts");
//				if (minecraftAccounts != null) {
//					return minecraftAccounts
//							.getAsJsonArray()
//							.asList()
//							.parallelStream()
//							.map(accountJson -> {
//								if (!accountJson.isJsonPrimitive() || !accountJson.getAsJsonPrimitive().isString()) {
//									return null;
//								}
//								String uuid = accountJson.getAsString();
//								String username = MinecraftAccountUtil.getMinecraftUsernameFromUuid(uuid);
//								if (username != null) {
//									return new Command.Choice(username, username);
//								}
//								return new Command.Choice(uuid, uuid);
//							}).filter(Objects::nonNull).toList();
//				}
//			}
//		} catch (IOException | InterruptedException e) {
//			GardenBot.LOG.error("Could not get Minecraft accounts from user.", e);
//		}
//		return Collections.emptyList();
//	}
}
