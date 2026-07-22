package net.modgarden.gardenbot.command.image;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.modgarden.gardenbot.GardenBot;
import net.modgarden.gardenbot.client.BunnyCdn;
import net.modgarden.gardenbot.client.Discord;
import net.modgarden.gardenbot.client.ModGarden;
import net.modgarden.gardenbot.client.exception.HypertextException;
import net.modgarden.gardenbot.client.mod_garden.event.GenreAndEvent;
import net.modgarden.gardenbot.client.mod_garden.event.ModGardenEvent;
import net.modgarden.gardenbot.client.mod_garden.event.ModGardenGenre;
import net.modgarden.gardenbot.client.mod_garden.user.ModGardenUser;
import net.modgarden.gardenbot.command.SlashCommand;
import net.modgarden.gardenbot.command.SlashCommandOption;
import net.modgarden.gardenbot.database.data.NaturalId;
import net.modgarden.gardenbot.interaction.SlashCommandInteraction;
import net.modgarden.gardenbot.response.EmbedResponse;
import net.modgarden.gardenbot.response.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;

public class ImageUploadCommand extends SlashCommand {
	public ImageUploadCommand() {
		super(
				"upload",
				"Uploads a PNG image to the Mod Garden CDN.",
				new SlashCommandOption(
						OptionType.ATTACHMENT,
						"attachment",
						"The image to upload.",
						true
				)
		);
	}

	@NotNull
	@Override
	public Response respond(SlashCommandInteraction interaction) throws HypertextException {
		interaction.event().deferReply(false).queue();
		User user = interaction.event().getUser();

		if (interaction.event().getChannelId() == null || !interaction.event().getChannelId().equals(GardenBot.DOTENV.get("IMAGE_CHANNEL_ID"))) {
			return new EmbedResponse()
					.setTitle("Failed to upload image to Mod Garden's CDN.")
					.setDescription("You may not use this command in this channel.")
					.markEphemeral(true)
					.setColor(0x5D3E40);
		}

		GenreAndEvent activeEvent = ModGarden.getActiveEvent();

		if (activeEvent == null) {
			return new EmbedResponse()
					.setTitle("Failed to upload image to Mod Garden's CDN.")
					.setDescription("There is no active event to upload images for.")
					.markEphemeral()
					.setColor(0x5D3E40);
		}

		ModGardenGenre genre = activeEvent.genre();
		ModGardenEvent event = activeEvent.event();

		Message.Attachment attachment = interaction.event().getOption("attachment", OptionMapping::getAsAttachment);

		if (attachment == null || !attachment.isImage() || attachment.getContentType() == null || (!attachment.getContentType().equals("image/png") && !attachment.getContentType().equals("image/webp"))) {
			return new EmbedResponse()
					.setTitle("Failed to upload image to Mod Garden's CDN.")
					.setDescription("Attachment must be a PNG.")
					.markEphemeral()
					.setColor(0x5D3E40);
		}

		ModGardenUser mgUser = ModGarden.getUserByDiscordUser(user);
		if (mgUser == null) {
			return new EmbedResponse()
					.setTitle("Failed to upload image to Mod Garden's CDN.")
					.setDescription("""
							You do not have a Mod Garden account.
							Please create one with **/account create**.""")
					.markEphemeral()
					.setColor(0x5D3E40);
		}

		if (!mgUser.events().contains(event.id())) {
			return new EmbedResponse()
					.setTitle("Failed to upload image to Mod Garden's CDN.")
					.setDescription("You are not a participant of " + event.metadata().name() + ".")
					.markEphemeral()
					.setColor(0x5D3E40);
		}


		StringBuilder fileNameBuilder = new StringBuilder()
				.append("v1/event/")
				.append(genre.slug())
				.append("/")
				.append(event.slug());

		String fileName;
		try {
			fileName = NaturalId.generateCdnLink(fileNameBuilder.toString(), "png", 5);
		} catch (IOException | InterruptedException e) {
			return new EmbedResponse()
					.setTitle("Failed to upload image to Mod Garden's CDN.")
					.setDescription("Internal error: Failed to generate natural ID for image path.")
					.markEphemeral()
					.setColor(0x5D3E40);
		}

		InputStream attachmentStream = Discord.attachmentToPngStream(attachment);
		BunnyCdn.upload(fileName, attachmentStream);

		return new EmbedResponse()
				.setTitle("Successfully uploaded image to Mod Garden's CDN")
				.setDescription("Your image may be found at\n<https://cdn.modgarden.net/" + fileName + ">")
				.setColor(0xA9FFA7);
	}
}
