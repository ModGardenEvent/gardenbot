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

public class EventRegisterCommand extends SlashCommand {
	public EventRegisterCommand() {
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
			ModGardenEvent event = ModGarden.getRegistrableEvent();
			if (event == null) {
				return new MessageResponse("No Mod Garden event is currently open for registration.")
						.markEphemeral();
			}

			ModGardenUser modGardenUser = ModGarden.getUserByDiscordId(user);
			if (modGardenUser == null) {
				return new MessageResponse("""
						You do not have a Mod Garden account.
						Please create one with **/account create**."""
				).markEphemeral();
			}

			ModGardenRole participantRole = ModGarden.getParticipantRole(event);
			if (participantRole == null) {
				return exceptionResponse("Participant role does not exist within event '" + event.metadata().name() + "'.");
			}

			if (guild != null && guild.getId().equals(GardenBot.DOTENV.get("GUILD_ID"))) {
				Role discordRole = guild.getRoleById(participantRole.integrations().discord().roleId());
				if (discordRole == null) {
					return exceptionResponse("The Discord integration for the event's role does not exist within the Mod Garden Discord '" + event.metadata().name() + "'.");
				}

				if (guild.getMembersWithRoles(discordRole).contains(interaction.event().getMember())) {
					return new MessageResponse(
							"You are already %s %s participant."
									.formatted(aOrAn(event.metadata().name()), event.metadata().name())
					).markEphemeral();
				}

				ModGarden.addUserRole(modGardenUser, participantRole);
				guild.addRoleToMember(user, discordRole).queue();

				return new MessageResponse(
						"Successfully registered you as a participant to %s."
								.formatted(event.metadata().name())
				).markEphemeral();
			}
		} catch (HypertextException e) {
			GardenBot.LOG.error("Exception whilst attempting to register a user to an event. ", e);
			return exceptionResponse(e.getStatus() + ": " + e.getMessage());
		}

		return new MessageResponse("This command must be run inside the Mod Garden Discord server.")
				.markEphemeral();
	}
}
