package net.modgarden.gardenbot.command.event;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.modgarden.gardenbot.GardenBot;
import net.modgarden.gardenbot.client.exception.HypertextException;
import net.modgarden.gardenbot.client.modgarden.event.ModGardenEvent;
import net.modgarden.gardenbot.client.modgarden.role.ModGardenRole;
import net.modgarden.gardenbot.client.modgarden.user.ModGardenUser;
import net.modgarden.gardenbot.command.SlashCommand;
import net.modgarden.gardenbot.interaction.SlashCommandInteraction;
import net.modgarden.gardenbot.response.MessageResponse;
import net.modgarden.gardenbot.response.Response;
import net.modgarden.gardenbot.client.ModGarden;
import org.jetbrains.annotations.NotNull;

import static net.modgarden.gardenbot.util.MiscUtil.aOrAn;

public class EventUnregisterCommand extends SlashCommand {
	public EventUnregisterCommand() {
		super("unregister", "Unregisters you from the current Mod Garden event.");
	}

	@NotNull
	@Override
	public Response respond(SlashCommandInteraction interaction) {
		interaction.event().deferReply(true).queue();
		User user = interaction.event().getUser();
		Guild guild = interaction.event().getGuild();

		try {
			ModGardenUser mgUser = ModGarden.getUserByDiscordId(user);

			if (mgUser == null) {
				return new MessageResponse("You do not have a Mod Garden account.\nPlease create one with **/account create**.")
						.markEphemeral();
			}

			if (guild != null && guild.getId().equals(GardenBot.DOTENV.get("GUILD_ID"))) {
				ModGardenEvent event = ModGarden.getRegistrableEvent().event();
				if (event == null) {
					return new MessageResponse("No Mod Garden event is currently open for registration.")
							.markEphemeral();
				}

				ModGardenRole mgRole = ModGarden.getParticipantRole(event);

				if (mgRole == null) {
					return exceptionResponse("Participant role does not exist within event '" + event.metadata().name() + "'.");
				}

				Role role = guild.getRoleById(mgRole.integrations().discord().roleId());

				if (role == null) {
					return exceptionResponse("Non-existent Discord role.");
				}

				if (mgUser.roles().contains(mgRole.id()) || guild.getMembersWithRoles(role).contains(interaction.event().getMember())) {
					ModGarden.removeUserRole(mgUser, mgRole);
					guild.removeRoleFromMember(user, role).queue();

					return new MessageResponse("Successfully removed you as a participant from %s."
							.formatted(event.metadata().name())
					).markEphemeral();
				}

				return new MessageResponse("You not %s %s participant."
						.formatted(aOrAn(event.metadata().name()), event.metadata().name())
				).markEphemeral();
			}
		} catch (HypertextException ex) {
			GardenBot.LOG.error("", ex);
			return exceptionResponse(ex.getMessage());
		}

		return new MessageResponse("This command must be run inside the Mod Garden Discord server.")
				.markEphemeral();
	}
}
