package net.modgarden.gardenbot.commands.image;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.modgarden.gardenbot.GardenBot;
import net.modgarden.gardenbot.interaction.SlashCommandInteraction;
import net.modgarden.gardenbot.interaction.response.EmbedResponse;
import net.modgarden.gardenbot.interaction.response.Response;
import net.modgarden.gardenbot.util.BunnyCDNAPIClient;
import net.modgarden.gardenbot.util.ModGardenAPIClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Random;

public class UploadHandler {
	public static Response handleUpload(SlashCommandInteraction interaction) {
		if (interaction.event().getChannelId() == null || !interaction.event().getChannelId().equals(GardenBot.DOTENV.get("IMAGE_CHANNEL_ID"))) {
			return new EmbedResponse()
					.setTitle("Failed to upload image to Mod Garden's CDN.")
					.setDescription("You may not use this command in this channel.")
					.markEphemeral(true)
					.setColor(0x5D3E40);
		}


		ModGardenEvent event;
		Message.Attachment attachment = interaction.event().getOption("attachment", OptionMapping::getAsAttachment);

		if (attachment == null || !attachment.isImage() || attachment.getContentType() == null || !attachment.getContentType().equals("image/png")) {
			return new EmbedResponse()
					.setTitle("Failed to upload image to Mod Garden's CDN.")
					.setDescription("Attachment must be a PNG.")
					.markEphemeral()
					.setColor(0x5D3E40);
		}

		try {
			var eventResult = ModGardenAPIClient.get("events/current/prefreeze", HttpResponse.BodyHandlers.ofInputStream());
			if (eventResult.statusCode() != 200) {
				return new EmbedResponse()
						.setTitle("Failed to upload image to Mod Garden's CDN.")
						.setDescription("There is no active event to upload images for.")
						.markEphemeral()
						.setColor(0x5D3E40);
			}
			try (InputStreamReader eventReader = new InputStreamReader(eventResult.body())) {
				event = GardenBot.GSON.fromJson(eventReader, ModGardenEvent.class);
			}
		} catch (IOException | InterruptedException ex) {
			GardenBot.LOG.error("", ex);
			return new EmbedResponse()
					.setTitle("Failed to upload image to Mod Garden's CDN.")
					.setDescription(ex.getMessage() + "\nPlease report this to a team member.")
					.markEphemeral()
					.setColor(0xFF0000);
		}

		ModGardenUser user;
		try {
			var userResult = ModGardenAPIClient.get("user/" + interaction.event().getUser().getId() + "?service=discord", HttpResponse.BodyHandlers.ofInputStream());
			if (userResult.statusCode() != 200) {
				return new EmbedResponse()
						.setTitle("Failed to upload image to Mod Garden's CDN.")
						.setDescription("You do not have a Mod Garden account. Please create one with **/register**.")
						.markEphemeral()
						.setColor(0x5D3E40);
			}

			// Validate that the user has submitted to the specified event.
			try (InputStreamReader userReader = new InputStreamReader(userResult.body())) {
				user = GardenBot.GSON.fromJson(userReader, ModGardenUser.class);
				if (!user.events.contains(event.id)) {
					return new EmbedResponse()
							.setTitle("Failed to upload image to Mod Garden's CDN.")
							.setDescription("You are not associated with any projects within " + event.displayName + ".")
							.markEphemeral()
							.setColor(0x5D3E40);
				}
			}
		} catch (IOException | InterruptedException ex) {
			GardenBot.LOG.error("", ex);
			return new EmbedResponse()
					.setTitle("Encountered an exception whilst attempting to upload an image to Mod Garden's CDN.")
					.setDescription(ex.getMessage() + "\nPlease report this to a team member.")
					.markEphemeral()
					.setColor(0xFF0000);
		}
		StringBuilder fileNameBuilder = new StringBuilder()
				.append(event.slug);

		String hash;
		try {
			hash = selectUniqueHash(fileNameBuilder.toString());
		} catch (NullPointerException | IOException | InterruptedException ex) {
			GardenBot.LOG.error("", ex);
			return new EmbedResponse()
					.setTitle("Encountered an exception whilst attempting to upload an image to Mod Garden's CDN.")
					.setDescription(ex.getMessage() + "\nPlease report this to a team member.")
					.markEphemeral()
					.setColor(0xFF0000);
		}

		fileNameBuilder
				.append("/")
				.append(hash)
				.append(".png");

		try (InputStream attachmentStream = attachment.getProxy().download(attachment.getWidth(), attachment.getHeight()).join()) {
			HttpResponse<InputStream> uploadResponse = BunnyCDNAPIClient.put(
					"public/" + fileNameBuilder,
					HttpRequest.BodyPublishers.ofInputStream(() -> attachmentStream),
					HttpResponse.BodyHandlers.ofInputStream(),
					"Content-Type", "application/octet-stream",
					"Accept", "image/png"
			);
			try (InputStreamReader streamReader = new InputStreamReader(uploadResponse.body())) {
				if (uploadResponse.statusCode() != 201) {
					JsonElement json = JsonParser.parseReader(streamReader);
					String errorMessage = json.isJsonObject() && json.getAsJsonObject().has("Message") ?
							json.getAsJsonObject().getAsJsonPrimitive("Message").getAsString() :
							"Undefined Error.";
					return new EmbedResponse()
							.setTitle("Encountered an exception whilst attempting to upload an image to Mod Garden's CDN.")
							.setDescription(uploadResponse.statusCode() + ": " + errorMessage + "\nPlease report this to a team member.")
							.markEphemeral()
							.setColor(0xFF0000);
				}
			}
		} catch (IOException | InterruptedException ex) {
			GardenBot.LOG.error("", ex);
			return new EmbedResponse()
					.setTitle("Encountered an exception whilst attempting to upload an image to Mod Garden's CDN.")
					.setDescription(ex.getMessage() + "\nPlease report this to a team member.")
					.markEphemeral()
					.setColor(0xFF0000);
		}

		return new EmbedResponse()
				.setTitle("Successfully uploaded image to Mod Garden's CDN")
				.setDescription("Your image may be found at\n<https://cdn.modgarden.net/public/" + fileNameBuilder + ">")
				.setColor(0xA9FFA7);
	}

	public static String selectUniqueHash(String basePath) throws IOException, InterruptedException {
		for (int i = 0; i < 20; ++i) {
			String hash = generateHash();
			HttpResponse<String> getResponse = BunnyCDNAPIClient.get(
					basePath + "/" + hash + ".png",
					HttpResponse.BodyHandlers.ofString()
			);
			if (getResponse.statusCode() != 200)
				return hash;
		}
		throw new NullPointerException("Failed to generate a unique upload hash. You should probably try again.");
	}

	@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
	private static class ModGardenUser {
		List<String> events;
	}

	private static class ModGardenEvent {
		String id;
		String slug;
		@SerializedName("display_name")
		String displayName;
	}

	private static final String VALID_UNIQUE_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
	private static final Random RANDOM = new Random();

	private static String generateHash() {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < 6; ++i) {
			builder.append(VALID_UNIQUE_CHARS.charAt(RANDOM.nextInt(VALID_UNIQUE_CHARS.length())));
		}
		return builder.toString();
	}
}
