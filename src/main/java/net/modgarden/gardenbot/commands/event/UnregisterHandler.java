package net.modgarden.gardenbot.commands.event;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.modgarden.gardenbot.GardenBot;
import net.modgarden.gardenbot.interaction.SlashCommandInteraction;
import net.modgarden.gardenbot.interaction.response.EmbedResponse;
import net.modgarden.gardenbot.interaction.response.MessageResponse;
import net.modgarden.gardenbot.interaction.response.Response;
import net.modgarden.gardenbot.util.ModGardenAPIClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.http.HttpResponse;

public class UnregisterHandler {
	// TODO: Unhardcode from Mod Garden: Nature and save role data into backend event.
	// TODO: Rewrite this entire thing too, it's very out of date...
	public static Response handleEventUnregister(SlashCommandInteraction interaction) {
		interaction.event().deferReply(true).queue();
		User discordUser = interaction.event().getUser();
		Guild guild = interaction.event().getGuild();

		ModGardenUser user;
		try {
			HttpResponse<InputStream> userResult = ModGardenAPIClient.get("user/" + discordUser.getId() + "?service=discord", HttpResponse.BodyHandlers.ofInputStream());
			if (userResult.statusCode() != 200) {
				return new MessageResponse()
						.setMessage("You do not have a Mod Garden account.\nPlease create one with **/account create**.")
						.markEphemeral();
			}
			try (InputStreamReader userReader = new InputStreamReader(userResult.body())) {
				user = GardenBot.GSON.fromJson(userReader, ModGardenUser.class);
			}
		} catch (IOException | InterruptedException ex) {
			GardenBot.LOG.error("", ex);
			return new EmbedResponse()
					.setTitle("Encountered an exception whilst attempting to unregister from a Mod Garden event.")
					.setDescription(ex.getMessage() + "\nPlease report this to a team member.")
					.setColor(0xFF0000);
		}

		try {
			HttpResponse<InputStream> eventSubmissionsResult = ModGardenAPIClient.get(
					"user/" + user.id + "/submissions/mod-garden-nature",
					HttpResponse.BodyHandlers.ofInputStream()
			);
			if (eventSubmissionsResult.statusCode() == 200) {
				try (InputStreamReader eventSubmissionsReader = new InputStreamReader(eventSubmissionsResult.body())) {
					JsonElement eventSubmissions = JsonParser.parseReader(eventSubmissionsReader);
					if (eventSubmissions.isJsonArray() && !eventSubmissions.getAsJsonArray().isEmpty()) {
						return new MessageResponse()
								.setMessage("You may not unregister whilst you have submissions to Mod Garden: Nature.")
								.markEphemeral();
					}
				}
			}
		}catch (IOException | InterruptedException ex) {
			GardenBot.LOG.error("", ex);
		}

		if (guild != null && guild.getId().equals(GardenBot.DOTENV.get("GUILD_ID"))) {
			Role role = guild.getRoleById("1320329531990741053");
			if (role == null) {
				return new MessageResponse()
						.setMessage("Non-existent role, please report this to a team member.")
						.markEphemeral();
			}
			if (!guild.getMembersWithRoles(role).contains(interaction.event().getMember())) {
				return new MessageResponse()
						.setMessage("You not a Mod Garden: Nature participant.")
						.markEphemeral();
			}
			guild.removeRoleFromMember(discordUser, role).complete();
			return new MessageResponse()
					.setMessage("Successfully removed you as a participant from Mod Garden: Nature.")
					.markEphemeral();
		}
		return new MessageResponse()
				.setMessage("This command must be run inside the Mod Garden Discord server.")
				.markEphemeral();
	}

	private static class ModGardenUser {
		String id;
	}
}
