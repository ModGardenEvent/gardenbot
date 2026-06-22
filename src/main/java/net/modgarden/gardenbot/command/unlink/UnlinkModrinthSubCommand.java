package net.modgarden.gardenbot.command.unlink;

import net.modgarden.gardenbot.command.SlashCommand;
import net.modgarden.gardenbot.interaction.SlashCommandInteraction;
import net.modgarden.gardenbot.response.MessageResponse;
import net.modgarden.gardenbot.response.Response;
import org.jetbrains.annotations.NotNull;

// TODO: Add functionality to Backend V2.
public class UnlinkModrinthSubCommand extends SlashCommand {
	public UnlinkModrinthSubCommand() {
		super(
				"modrinth",
				"Unlinks your Modrinth account from Mod Garden"
		);
	}

	@NotNull
	@Override
	public Response respond(SlashCommandInteraction interaction) {
		return new MessageResponse("Account integrations are not yet implemented.");

//		return new EmbedResponse()
//				.setTitle("Are you sure?")
//				.setDescription("Are you sure you want to unlink your current Modrinth account?")
//				.setColor(0X5D3E40)
//				.addButton(
//						"unlinkModrinth",
//						"Unlink",
//						ButtonStyle.DANGER,
//						Emoji.fromUnicode("U+26D3U+FE0FU+200DU+1F4A5")
//				).markEphemeral();
	}
}
