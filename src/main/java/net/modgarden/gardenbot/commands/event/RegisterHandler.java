package net.modgarden.gardenbot.commands.event;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.modgarden.gardenbot.GardenBot;
import net.modgarden.gardenbot.interaction.SlashCommandInteraction;
import net.modgarden.gardenbot.interaction.response.MessageResponse;
import net.modgarden.gardenbot.interaction.response.Response;
import net.modgarden.gardenbot.util.ModGardenAPIClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpResponse;

public class RegisterHandler {
	public static Response handleEventRegister(SlashCommandInteraction interaction) {
		interaction.event().deferReply(true).queue();
		User user = interaction.event().getUser();
		Guild guild = interaction.event().getGuild();

		try {
			HttpResponse<InputStream> stream = ModGardenAPIClient.get("user/" + user.getId() + "?service=discord", HttpResponse.BodyHandlers.ofInputStream());
			if (stream.statusCode() != 200) {
				return new MessageResponse()
						.setMessage("You do not have a Mod Garden account.\nPlease create one with **/account create**.")
						.markEphemeral();
			}
		} catch (IOException | InterruptedException ex) {
			GardenBot.LOG.error("", ex);
		}

		if (guild != null && guild.getId().equals(GardenBot.DOTENV.get("GUILD_ID"))) {
			// TODO: Unhardcode from Mod Garden: Nature and save data into backend user.
			Role role = guild.getRoleById("1320329531990741053");
			if (role == null) {
				return new MessageResponse()
						.setMessage("Non-existent role, please report this to a team member.")
						.markEphemeral();
			}
			if (guild.getMembersWithRoles(role).contains(interaction.event().getMember())) {
				return new MessageResponse()
						.setMessage("You are already a Mod Garden: Nature participant.")
						.markEphemeral();
			}
			guild.addRoleToMember(user, role).complete();
			return new MessageResponse()
					.setMessage("Successfully added you as a participant to Mod Garden: Nature.")
					.markEphemeral();
		}
		return new MessageResponse()
				.setMessage("This command needs to be run inside the Mod Garden Discord server.")
				.markEphemeral();
	}
}
