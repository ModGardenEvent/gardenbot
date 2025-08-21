package net.modgarden.gardenbot;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.dv8tion.jda.api.entities.User;
import net.modgarden.gardenbot.interaction.ButtonInteraction;
import net.modgarden.gardenbot.interaction.button.ButtonDispatcher;
import net.modgarden.gardenbot.interaction.response.EmbedResponse;
import net.modgarden.gardenbot.interaction.response.MessageResponse;
import net.modgarden.gardenbot.interaction.response.ModalResponse;
import net.modgarden.gardenbot.interaction.response.Response;
import net.modgarden.gardenbot.util.MinecraftAccountUtil;
import net.modgarden.gardenbot.util.ModGardenAPIClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class GardenBotButtonHandlers {
	public static void registerAll() {
		ButtonDispatcher.register("linkModrinth", new ModalResponse(GardenBotModals.LINK_MODRINTH));
		ButtonDispatcher.register("unlinkModrinth", GardenBotButtonHandlers::unlinkModrinth);
		ButtonDispatcher.register("linkMinecraft", new ModalResponse(GardenBotModals.LINK_MINECRAFT));
		ButtonDispatcher.register("unlinkMinecraft?%s", GardenBotButtonHandlers::unlinkMinecraft);

		ButtonDispatcher.register("acceptInvite?%s", GardenBotButtonHandlers::acceptInvite);
		ButtonDispatcher.register("declineInvite?%s", GardenBotButtonHandlers::declineInvite);
	}

	public static Response unlinkModrinth(ButtonInteraction interaction) {
		User user = interaction.event().getUser();

		JsonObject inputJson = new JsonObject();
		inputJson.addProperty("discord_id", user.getId());
		inputJson.addProperty("service", "modrinth");

		try {
			HttpResponse<InputStream> stream = ModGardenAPIClient.post(
					"discord/unlink",
					HttpRequest.BodyPublishers.ofString(inputJson.toString()),
					HttpResponse.BodyHandlers.ofInputStream(),
					"Content-Type", "application/json"
			);
			if (stream.statusCode() < 200 || stream.statusCode() > 299) {
				JsonElement json = JsonParser.parseReader(new InputStreamReader(stream.body()));
				String errorDescription = json.isJsonObject() && json.getAsJsonObject().has("description") ?
						json.getAsJsonObject().getAsJsonPrimitive("description").getAsString() :
						"Undefined Error.";
				return new EmbedResponse()
						.setTitle("Encountered an exception whilst attempting to unlink your Modrinth account from your Mod Garden account.")
						.setDescription(stream.statusCode() + ": " + errorDescription + "\nPlease report this to a team member.")
						.setColor(0xFF0000)
						.markEphemeral();
			}
		} catch (IOException | InterruptedException ex) {
			GardenBot.LOG.error("", ex);
			return new EmbedResponse()
					.setTitle("Encountered an exception whilst attempting to unlink your Modrinth account from your Mod Garden account.")
					.setDescription(ex.getMessage() + "\nPlease report this to a team member.")
					.setColor(0xFF0000)
					.markEphemeral();
		}

		return new EmbedResponse()
				.setTitle("Successfully unlinked your Modrinth account from Mod Garden!")
				.setColor(0xA9FFA7)
				.markEphemeral();
	}

	public static Response unlinkMinecraft(ButtonInteraction interaction) {
		User user = interaction.event().getUser();
		String uuid = interaction.arguments()[0];

		JsonObject inputJson = new JsonObject();
		inputJson.addProperty("discord_id", user.getId());
		inputJson.addProperty("service", "minecraft");
		inputJson.addProperty("minecraft_uuid", uuid);

		try {
			HttpResponse<InputStream> stream = ModGardenAPIClient.post(
					"discord/unlink",
					HttpRequest.BodyPublishers.ofString(inputJson.toString()),
					HttpResponse.BodyHandlers.ofInputStream(),
					"Content-Type", "application/json"
			);
			if (stream.statusCode() < 200 || stream.statusCode() > 299) {
				JsonElement json = JsonParser.parseReader(new InputStreamReader(stream.body()));
				String errorDescription = json.isJsonObject() && json.getAsJsonObject().has("description") ?
						json.getAsJsonObject().getAsJsonPrimitive("description").getAsString() :
						"Undefined Error.";
				return new EmbedResponse()
						.setTitle("Encountered an exception whilst attempting to unlink your Minecraft account from your Mod Garden account.")
						.setDescription(stream.statusCode() + ": " + errorDescription + "\nPlease report this to a team member.")
						.setColor(0xFF0000)
						.markEphemeral();
			}
			if (stream.statusCode() == 200) {
				return new MessageResponse()
						.setMessage("You have already linked this Minecraft account to your Mod Garden account.")
						.markEphemeral();
			}
		} catch (IOException | InterruptedException ex) {
			GardenBot.LOG.error("", ex);
			return new EmbedResponse()
					.setTitle("Encountered an exception whilst attempting to unlink your Minecraft account from your Mod Garden account.")
					.setDescription(ex.getMessage() + "\nPlease report this to a team member.")
					.setColor(0xFF0000)
					.markEphemeral();
		}

		String username = MinecraftAccountUtil.getMinecraftUsernameFromUuid(uuid);
		if (username == null) {
			username = uuid;
		}

		return new EmbedResponse()
				.setTitle("Successfully unlinked your Minecraft account (" + username + ") from Mod Garden!")
				.setColor(0xA9FFA7)
				.markEphemeral();
	}

	public static Response acceptInvite(ButtonInteraction interaction) {
		String inviteCode = interaction.arguments()[0];

		JsonObject publisher = new JsonObject();
		publisher.addProperty("invite_code", inviteCode);

		try {
			HttpResponse<Void> acceptResult = ModGardenAPIClient.post(
					"discord/project/user/accept",
					HttpRequest.BodyPublishers.ofString(publisher.toString()),
					HttpResponse.BodyHandlers.discarding(),
					"Content-Type", "application/json"
			);
			if (acceptResult.statusCode() != 201) {
				return new EmbedResponse()
						.setTitle("Could not accept invite to project.")
						.setDescription("This invite is invalid or has expired.")
						.setColor(0x5D3E40)
						.markEphemeral();
			}
		} catch (IOException | InterruptedException ex) {
			GardenBot.LOG.error("", ex);
			return new EmbedResponse()
					.setTitle("Encountered an exception whilst attempting to accept invite.")
					.setDescription(ex.getMessage() + "\nPlease report this to a team member.")
					.setColor(0xFF0000)
					.markEphemeral();
		}

		return new EmbedResponse()
				.setTitle("Successfully accepted invite to project!")
				.setColor(0xA9FFA7)
				.markEphemeral();
	}

	public static Response declineInvite(ButtonInteraction interaction) {
		String inviteCode = interaction.arguments()[0];

		JsonObject publisher = new JsonObject();
		publisher.addProperty("invite_code", inviteCode);

		try {
			HttpResponse<Void> declineResult = ModGardenAPIClient.post(
					"discord/project/user/decline",
					HttpRequest.BodyPublishers.ofString(publisher.toString()),
					HttpResponse.BodyHandlers.discarding(),
					"Content-Type", "application/json"
			);
			if (declineResult.statusCode() != 201) {
				return new EmbedResponse()
						.setTitle("Could not decline invite to project.")
						.setDescription("This invite is invalid or has expired.")
						.setColor(0x5D3E40)
						.markEphemeral();
			}
		} catch (IOException | InterruptedException ex) {
			GardenBot.LOG.error("", ex);
			return new EmbedResponse()
					.setTitle("Encountered an exception whilst attempting to decline invite.")
					.setDescription(ex.getMessage() + "\nPlease report this to a team member.")
					.setColor(0xFF0000)
					.markEphemeral();
		}

		return new EmbedResponse()
				.setTitle("Successfully declined invite to project!")
				.setColor(0xA9FFA7)
				.markEphemeral();
	}
}
