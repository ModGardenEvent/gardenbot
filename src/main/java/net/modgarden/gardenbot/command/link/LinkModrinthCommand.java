package net.modgarden.gardenbot.command.link;

import net.modgarden.gardenbot.command.SlashCommand;
import net.modgarden.gardenbot.interaction.SlashCommandInteraction;
import net.modgarden.gardenbot.response.MessageResponse;
import net.modgarden.gardenbot.response.Response;
import org.jetbrains.annotations.NotNull;

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
		return new MessageResponse("Account integrations are not yet implemented.");
//		return new EmbedResponse()
//				.setTitle("Link your Modrinth Account!")
//				.setDescription(
//						"""
//						1. Authorize with Modrinth, which will redirect you to a page with a link code.
//						2. 2. Enter your link code inside the modal.
//
//						You may only have one Modrinth account linked to your Mod Garden account.
//						""")
//				.setColor(0xA9FFA7)
//				.addButtonUrl(
//						URI.create("""
//							https://modrinth.com/auth/authorize
//								?client_id=Q2tuKyb4
//								&redirect_uri=%s
//								&scope=USER_READ+PROJECT_READ+VERSION_READ+ORGANIZATION_READ"""
//								.formatted(GardenBot.API_URL + "/discord/oauth/modrinth")
//						),
//						"1. Authorize",
//						Emoji.fromCustom("modrinth", 1330663190626828479L, false)
//				)
//				.addButton(
//						"linkModrinth",
//						"2. Link",
//						ButtonStyle.SECONDARY,
//						Emoji.fromUnicode("U+1F517")
//				).markEphemeral();
	}
}
