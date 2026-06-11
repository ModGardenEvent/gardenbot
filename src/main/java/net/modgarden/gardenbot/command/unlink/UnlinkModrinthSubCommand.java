package net.modgarden.gardenbot.command.unlink;

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
import net.modgarden.gardenbot.client.ModGarden;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.http.HttpResponse;

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
		return new MessageResponse("Account integrations not currently implemented.");

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
