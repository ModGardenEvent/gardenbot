package net.modgarden.gardenbot.command.event;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.modgarden.gardenbot.GardenBot;
import net.modgarden.gardenbot.command.SlashCommand;
import net.modgarden.gardenbot.interaction.SlashCommandInteraction;
import net.modgarden.gardenbot.response.EmbedResponse;
import net.modgarden.gardenbot.response.MessageResponse;
import net.modgarden.gardenbot.response.Response;
import net.modgarden.gardenbot.util.ModGardenAPIClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static net.modgarden.gardenbot.util.MiscUtil.aOrAn;

public class RegisterCommand extends SlashCommand {
	public RegisterCommand() {
		super(
				"register",
				"Registers you to the currently active Mod Garden event."
		);
	}

	@NotNull
	@Override
	public Response respond(SlashCommandInteraction interaction) {
		interaction.event().deferReply(true).queue();
		User user = interaction.event().getUser();
		Guild guild = interaction.event().getGuild();

		try {
			ModGardenEvent modGardenEvent = getActiveEvent();
			if (modGardenEvent == null) {
				return new MessageResponse("No Mod Garden event is currently active.")
						.markEphemeral();
			}

			ModGardenUser modGardenUser = getModGardenUser(user);
			if (modGardenUser == null) {
				return new MessageResponse("""
						You do not have a Mod Garden account.
						Please create one with **/account create**."""
				).markEphemeral();
			}

			ModGardenUserRole participantRole = getParticipantRole(modGardenEvent);
			if (participantRole == null) {
				throw new IllegalStateException("Participant role does not exist within event '" + modGardenEvent.metadata.name + "'.");
			}

			if (guild != null && guild.getId().equals(GardenBot.DOTENV.get("GUILD_ID"))) {
				Role discordRole = guild.getRoleById(participantRole.integrations.discord.roleId);
				if (discordRole == null) {
					throw new IllegalStateException("The Discord integration for the event's role does not exist within the Mod Garden Discord '" + modGardenEvent.metadata.name + "'.");
				}

				if (guild.getMembersWithRoles(discordRole).contains(interaction.event().getMember())) {
					return new MessageResponse(
							"You are already %s %s participant."
									.formatted(aOrAn(modGardenEvent.metadata.name), modGardenEvent.metadata.name)
					).markEphemeral();
				}

				addUserRole(modGardenUser, participantRole);
				guild.addRoleToMember(user, discordRole).queue();

				return new MessageResponse(
						"Successfully added you as a participant to %s."
								.formatted(modGardenEvent.metadata.name)
				).markEphemeral();
			}

			return new MessageResponse("This command must be run inside the Mod Garden Discord server.")
					.markEphemeral();
		} catch (Exception e) {
			GardenBot.LOG.error("", e);
			return new EmbedResponse()
					.setTitle("Encountered an exception!")
					.setDescription(e.getMessage() + "\nPlease report this to a team member.")
					.markEphemeral()
					.setColor(0xFF0000);
		}
	}

	private static void addUserRole(ModGardenUser user, ModGardenUserRole role) throws IOException, InterruptedException {
		ModifyUserRequest request = new ModifyUserRequest();
		request.roles.add(role.id);
		JsonElement requestJson = GardenBot.GSON.toJsonTree(request, ModifyUserRequest.class);

		HttpResponse<InputStream> modifyStream = ModGardenAPIClient.patch(
				"internal/user/modify/" + user.id,
				HttpRequest.BodyPublishers.ofString(requestJson.toString()),
				HttpResponse.BodyHandlers.ofInputStream()
		);
		if (modifyStream.statusCode() != 200) {
			JsonElement errorJson = JsonParser.parseReader(new InputStreamReader(modifyStream.body()));
			String errorDescription = errorJson.isJsonObject() && errorJson.getAsJsonObject().has("description")
					? errorJson.getAsJsonObject().getAsJsonPrimitive("description").getAsString()
					: "Undefined Error.";
			throw new IllegalStateException(errorDescription);
		}
	}

	@Nullable
	private static ModGardenUser getModGardenUser(User user) throws IOException, InterruptedException {
		HttpResponse<InputStream> userStream = ModGardenAPIClient.get(
				"v2/users/" + user.getId() + "?by=integration_discord",
				HttpResponse.BodyHandlers.ofInputStream()
		);
		if (userStream.statusCode() != 200) {
			return null;
		}
		JsonElement userJson = JsonParser.parseReader(new InputStreamReader(userStream.body()));
		return GardenBot.GSON.fromJson(userJson, ModGardenUser.class);
	}

	@Nullable
	private static ModGardenUserRole getParticipantRole(ModGardenEvent event) throws IOException, InterruptedException {
		String participantRoleId = event.roles.participant;

		HttpResponse<InputStream> roleStream = ModGardenAPIClient.get(
				"v2/roles/" + participantRoleId,
				HttpResponse.BodyHandlers.ofInputStream()
		);
		if (roleStream.statusCode() != 200) {
			return null;
		}
		JsonElement userJson = JsonParser.parseReader(new InputStreamReader(roleStream.body()));
		return GardenBot.GSON.fromJson(userJson, ModGardenUserRole.class);
	}

	@Nullable
	private static ModGardenEvent getActiveEvent() throws IOException, InterruptedException {
		// TODO: Make an Active Events Endpoint
		HttpResponse<InputStream> eventsStream = ModGardenAPIClient.get(
				"v2/events/mod-garden",
				HttpResponse.BodyHandlers.ofInputStream()
		);
		if (eventsStream.statusCode() != 200) {
			return null;
		}

		JsonElement eventsJson = JsonParser.parseReader(new InputStreamReader(eventsStream.body()));
		for (JsonElement element : eventsJson.getAsJsonArray()) {
			ModGardenEvent event = GardenBot.GSON.fromJson(element, ModGardenEvent.class);

			long now = Instant.now().toEpochMilli();

			long registrationOpen = Long.parseLong(event.times.registrationOpen);
			long packFreeze = Long.parseLong(event.times.packFreeze);

			if (now >= registrationOpen && now < packFreeze) {
				return event;
			}
		}

		return null;
	}

	private static class ModGardenUser {
		public String id;
	}

	private static class ModGardenUserRole {
		public String id;
		public UserRoleIntegrations integrations;
	}

	private static class ModGardenEvent {
		public EventMetadata metadata;
		public EventTimes times;
		public EventRoles roles;
	}

	private static class EventMetadata {
		public String name;
	}

	private static class EventRoles {
		public String participant;
	}

	private static class UserRoleIntegrations {
		public DiscordIntegration discord;
	}

	private static class DiscordIntegration {
		@SerializedName("role_id")
		public String roleId;
	}

	private static class EventTimes {
		@SerializedName("registration_open")
		public String registrationOpen;

		@SerializedName("pack_freeze")
		public String packFreeze;
	}

	private static class ModifyUserRequest {
		public List<String> roles = new ArrayList<>();
	}
}
