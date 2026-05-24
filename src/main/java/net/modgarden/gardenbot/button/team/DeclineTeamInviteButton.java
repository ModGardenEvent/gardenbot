package net.modgarden.gardenbot.button.team;

import net.modgarden.gardenbot.GardenBot;
import net.modgarden.gardenbot.button.Button;
import net.modgarden.gardenbot.database.DatabaseAccess;
import net.modgarden.gardenbot.database.data.TeamInvite;
import net.modgarden.gardenbot.interaction.ButtonInteraction;
import net.modgarden.gardenbot.response.EmbedResponse;
import net.modgarden.gardenbot.response.MessageResponse;
import net.modgarden.gardenbot.response.Response;
import org.jetbrains.annotations.NotNull;

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

			return new MessageResponse("You have declined the invite to the Mod Garden project '" + invite.projectName() + "'.")
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
}
