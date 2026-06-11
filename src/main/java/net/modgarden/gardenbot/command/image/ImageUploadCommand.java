package net.modgarden.gardenbot.command.image;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.modgarden.gardenbot.GardenBot;
import net.modgarden.gardenbot.client.BunnyCdn;
import net.modgarden.gardenbot.client.Discord;
import net.modgarden.gardenbot.client.exception.HypertextException;
import net.modgarden.gardenbot.client.modgarden.event.GenreAndEvent;
import net.modgarden.gardenbot.client.modgarden.event.ModGardenEvent;
import net.modgarden.gardenbot.client.modgarden.event.ModGardenGenre;
import net.modgarden.gardenbot.client.modgarden.user.ModGardenUser;
import net.modgarden.gardenbot.command.SlashCommand;
import net.modgarden.gardenbot.command.SlashCommandOption;
import net.modgarden.gardenbot.database.data.NaturalId;
import net.modgarden.gardenbot.interaction.SlashCommandInteraction;
import net.modgarden.gardenbot.response.EmbedResponse;
import net.modgarden.gardenbot.response.Response;
import net.modgarden.gardenbot.client.ModGarden;
import org.jetbrains.annotations.NotNull;

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
	public Response respond(SlashCommandInteraction interaction) {
		User user = interaction.event().getUser();

		if (interaction.event().getChannelId() == null || !interaction.event().getChannelId().equals(GardenBot.DOTENV.get("IMAGE_CHANNEL_ID"))) {
			return new EmbedResponse()
					.setTitle("Failed to upload image to Mod Garden's CDN.")
					.setDescription("You may not use this command in this channel.")
					.markEphemeral(true)
					.setColor(0x5D3E40);
		}

		try {
			GenreAndEvent genreAndEvent = ModGarden.getActiveEvent();

			if (genreAndEvent == null) {
				return new EmbedResponse()
						.setTitle("Failed to upload image to Mod Garden's CDN.")
						.setDescription("There is no active event to upload images for.")
						.markEphemeral()
						.setColor(0x5D3E40);
			}

			ModGardenGenre genre = genreAndEvent.genre();
			ModGardenEvent event = genreAndEvent.event();

			Message.Attachment attachment = interaction.event().getOption("attachment", OptionMapping::getAsAttachment);

			if (attachment == null || !attachment.isImage() || attachment.getContentType() == null || (!attachment.getContentType().equals("image/png") && !attachment.getContentType().equals("image/webp"))) {
				return new EmbedResponse()
						.setTitle("Failed to upload image to Mod Garden's CDN.")
						.setDescription("Attachment must be a PNG.")
						.markEphemeral()
						.setColor(0x5D3E40);
			}

			ModGardenUser mgUser = ModGarden.getUserByDiscordId(user);
			if (mgUser == null) {
				return new EmbedResponse()
						.setTitle("Failed to upload image to Mod Garden's CDN.")
						.setDescription("You do not have a Mod Garden account. Please create one with **/register**.")
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
					.append(genre.slug())
					.append("/")
					.append(event.slug());

			String hash = NaturalId.generateCdnLink(fileNameBuilder.toString(), "png", 5);

			fileNameBuilder
					.append("/")
					.append(hash)
					.append(".png");

			InputStream attachmentStream = Discord.attachmentToPngStream(attachment);
			BunnyCdn.upload("public/" + fileNameBuilder, attachmentStream);

			return new EmbedResponse()
					.setTitle("Successfully uploaded image to Mod Garden's CDN")
					.setDescription("Your image may be found at\n<https://cdn.modgarden.net/public/" + fileNameBuilder + ">")
					.setColor(0xA9FFA7);
		} catch (HypertextException e) {
			GardenBot.LOG.error("", e);
			return exceptionResponse(e);
		} catch (Exception e) {
			GardenBot.LOG.error("", e);
			return exceptionResponse(e.getMessage());
		}

	}
}
