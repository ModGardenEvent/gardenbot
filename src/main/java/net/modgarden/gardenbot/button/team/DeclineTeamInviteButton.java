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
import java.net.http.HttpResponse;

public class DeclineTeamInviteButton extends Button {
	public DeclineTeamInviteButton() {
		super(
				"decline_team_invite",
				"invite_id"
		);
	}

	@NotNull
	@Override
	public Response respond(ButtonInteraction interaction) {
		String inviteId = interaction.arguments().get("invite_id");

		try {
			DatabaseAccess db = DatabaseAccess.get();
			TeamInvite invite = db.getTeamInvite(inviteId);

			if (invite == null) {
				return new MessageResponse("This invite has already been declined or has expired.")
						.markEphemeral();
			}

			db.revokeTeamInvite(inviteId);

			HttpResponse<InputStream> projectStream = ModGardenAPIClient.get(
					"v2/projects/" + invite.projectId(),
					HttpResponse.BodyHandlers.ofInputStream()
			);
			if (projectStream.statusCode() != 200) {
				return new MessageResponse("No action necessary. The project you were invited to does not exist.")
						.markEphemeral();
			}
			JsonElement projectJson = JsonParser.parseReader(new InputStreamReader(projectStream.body()));
			ModGardenProject modGardenProject = GardenBot.GSON.fromJson(projectJson, ModGardenProject.class);

			return new MessageResponse("You have declined the invite to the Mod Garden project '" + modGardenProject.metadata.name + "'.")
					.markEphemeral();
		} catch (Exception ex) {
			GardenBot.LOG.error("", ex);
			return new EmbedResponse()
					.setTitle("Encountered an exception whilst attempting to decline invite.")
					.setDescription(ex.getMessage() + "\nPlease report this to a team member.")
					.setColor(0xFF0000)
					.markEphemeral();
		}
	}

	private static class ModGardenProject {
		public ProjectMetadata metadata;
	}

	private static class ProjectMetadata {
		public String name;
	}
}
