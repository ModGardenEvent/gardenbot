package net.modgarden.gardenbot.button.team;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.modgarden.gardenbot.GardenBot;
import net.modgarden.gardenbot.button.Button;
import net.modgarden.gardenbot.database.DatabaseAccess;
import net.modgarden.gardenbot.database.data.TeamInvite;
import net.modgarden.gardenbot.interaction.ButtonInteraction;
import net.modgarden.gardenbot.response.EmbedResponse;
import net.modgarden.gardenbot.response.MessageResponse;
import net.modgarden.gardenbot.response.Response;
import net.modgarden.gardenbot.util.ModGardenAPIClient;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

public class AcceptTeamInviteButton extends Button {
	public AcceptTeamInviteButton() {
		super(
				"accept_team_invite",
				"invite_id"
		);
	}

	@NotNull
	@Override
	public Response respond(ButtonInteraction interaction) {
		interaction.event().deferReply(true).queue();
		String inviteId = interaction.arguments().get("invite_id");

		try {
			DatabaseAccess db = DatabaseAccess.get();
			TeamInvite invite = db.getTeamInvite(inviteId);

			if (invite == null) {
				return new MessageResponse("This invite is invalid or has expired.")
						.markEphemeral();
			}

			Map<String, String> request = new HashMap<>();
			request.put(invite.userId(), invite.role());

			JsonElement requestJson = GardenBot.GSON.toJsonTree(request, Map.class);

			HttpResponse<InputStream> projectStream = ModGardenAPIClient.get(
					"v2/projects/" + invite.projectId(),
					HttpResponse.BodyHandlers.ofInputStream()
			);
			if (projectStream.statusCode() != 200) {
				db.revokeTeamInvite(inviteId);
				// Not an internal error in the case of unsubmitted projects.
				return new MessageResponse("The project you were invited to does not exist.")
						.markEphemeral();
			}
			JsonElement projectJson = JsonParser.parseReader(new InputStreamReader(projectStream.body()));
			ModGardenProject modGardenProject = GardenBot.GSON.fromJson(projectJson, ModGardenProject.class);

			HttpResponse<InputStream> getInvitedUserStream = ModGardenAPIClient.get("v2/users/" + interaction.event().getUser().getId() + "?by=integration_discord", HttpResponse.BodyHandlers.ofInputStream());
			if (getInvitedUserStream.statusCode() != 200) {
				db.revokeTeamInvite(inviteId);
				// Also not an internal error in the case of deleted Mod Garden accounts.
				return new MessageResponse("""
						You do not have a Mod Garden account.
						Please create one with **/account create** inside the Mod Garden server.""")
						.markEphemeral();
			}
			JsonElement invitedUserJson = JsonParser.parseReader(new InputStreamReader(getInvitedUserStream.body()));
			String invitedUserId = GardenBot.GSON.fromJson(invitedUserJson, ModGardenUser.class).id;

			if (modGardenProject.team.containsKey(invitedUserId)) {
				db.revokeTeamInvite(inviteId);

				return new MessageResponse("You are already a member of the Mod Garden project '" + modGardenProject.metadata.name + "'.")
						.markEphemeral();
			}

			HttpResponse<InputStream> modifyMembers = ModGardenAPIClient.patch(
					"v2/projects/" + invite.projectId() + "/members",
					HttpRequest.BodyPublishers.ofString(requestJson.toString()),
					HttpResponse.BodyHandlers.ofInputStream()
			);

			if (modifyMembers.statusCode() != 200) {
				JsonElement errorJson = JsonParser.parseReader(new InputStreamReader(modifyMembers.body()));
				String description = errorJson.isJsonObject() && errorJson.getAsJsonObject().has("description")
						? errorJson.getAsJsonObject().get("description").getAsString()
						: "Unknown Exception";
				throw new IllegalStateException(description);
			}

			db.revokeTeamInvite(inviteId);

			return new MessageResponse("You are now a member of the Mod Garden project '" + modGardenProject.metadata.name + "'.")
					.markEphemeral();
		} catch (Exception ex) {
			GardenBot.LOG.error("", ex);
			return new EmbedResponse()
					.setTitle("Encountered an exception whilst attempting to accept invite.")
					.setDescription(ex.getMessage() + "\nPlease report this to a team member.")
					.setColor(0xFF0000)
					.markEphemeral();
		}
	}

	private static class ModGardenUser {
		public String id;
	}

	private static class ModGardenProject {
		public ProjectMetadata metadata;
		public Map<String, String> team;
	}

	private static class ProjectMetadata {
		public String name;
	}
}
