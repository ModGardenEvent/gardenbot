package net.modgarden.gardenbot.button.team;

import net.modgarden.gardenbot.GardenBot;
import net.modgarden.gardenbot.button.Button;
import net.modgarden.gardenbot.client.ModGarden;
import net.modgarden.gardenbot.client.modgarden.project.ModGardenProject;
import net.modgarden.gardenbot.client.modgarden.user.ModGardenUser;
import net.modgarden.gardenbot.database.DatabaseAccess;
import net.modgarden.gardenbot.database.data.TeamInvite;
import net.modgarden.gardenbot.interaction.ButtonInteraction;
import net.modgarden.gardenbot.response.EmbedResponse;
import net.modgarden.gardenbot.response.MessageResponse;
import net.modgarden.gardenbot.response.Response;
import org.jetbrains.annotations.NotNull;

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


			ModGardenProject mgProject = ModGarden.getProject(invite.projectId());
			if (mgProject == null) {
				db.revokeTeamInvite(inviteId);
				// Not an error in the case of unsubmitted projects.
				return new MessageResponse("The project you were invited to does not exist.")
						.markEphemeral();
			}

			ModGardenUser invitedUser = ModGarden.getUserByDiscordId(interaction.event().getUser());

			if (invitedUser == null) {
				db.revokeTeamInvite(inviteId);
				// Not an error in the case of deleted users in the future.
				return new MessageResponse("You do not have a Mod Garden account.\nPlease create one with **/account create**.")
						.markEphemeral();
			}

			if (mgProject.team().containsKey(invitedUser.id())) {
				db.revokeTeamInvite(inviteId);

				return new MessageResponse("You are already a member of the Mod Garden project '" + mgProject.metadata().name() + "'.")
						.markEphemeral();
			}

			Map<ModGardenUser, String> map = new HashMap<>();
			map.put(invitedUser, invite.role());

			ModGarden.addTeamMembers(mgProject, map);

			db.revokeTeamInvite(inviteId);

			return new MessageResponse("You are now a member of the Mod Garden project '" + mgProject.metadata().name() + "'.")
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
}
