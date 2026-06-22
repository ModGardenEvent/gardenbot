package net.modgarden.gardenbot.command.sudo;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.modgarden.gardenbot.GardenBot;
import net.modgarden.gardenbot.client.exception.HypertextException;
import net.modgarden.gardenbot.command.AutoCompletionGetter;
import net.modgarden.gardenbot.command.SlashCommand;
import net.modgarden.gardenbot.command.SlashCommandOption;
import net.modgarden.gardenbot.database.DatabaseAccess;
import net.modgarden.gardenbot.interaction.SlashCommandInteraction;
import net.modgarden.gardenbot.response.MessageResponse;
import net.modgarden.gardenbot.response.Response;
import net.modgarden.gardenbot.util.SchedulerUtil;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class SudoCommand extends SlashCommand {
	// Feel free to swap these out whilst testing.
	public static final String SUDO_ROLE_ID = "1441570757539790859"; // Mod Garden Server - sudo ID: '1441570757539790859'
	private static final String SUDOER_ROLE_ID = "1366207851261071402"; // Mod Garden Server - Server Manager ID: '1366207851261071402'

	private static final Map<Member, ScheduledFuture<?>> SUDOER_TIMERS = new HashMap<>();

	public SudoCommand() {
		super(
				"sudo",
				"Toggles or gives you the sudo role for a specific amount of time. (Requires Server Manager role)",
				new SlashCommandOption(
						OptionType.INTEGER,
						"time",
						"The amount of time (in minutes) you will have the sudo role. (Defaults to 30 minutes)",
						false,
						true
				)
		);
	}

	@NotNull
	@Override
	public Response respond(SlashCommandInteraction interaction) throws HypertextException, SQLException {
		interaction.event().deferReply(true).queue();
		Guild guild = interaction.event().getGuild();

		if (guild == null || !guild.getId().equals(GardenBot.GUILD_ID))
			return new MessageResponse("You must run this command within the Mod Garden Discord server!");


		Role sudoerRoleId = guild.getRoleById(SUDOER_ROLE_ID);

		if (interaction.event().getMember() == null || !interaction.event().getMember().getRoles().contains(sudoerRoleId)) {
			return new MessageResponse("You do not have the permissions to execute this command.");
		}

		Role sudoRole = guild.getRoleById(SUDO_ROLE_ID);
		assert sudoRole != null;

		// Wish we had nullable types...
		Integer time = interaction.event().getInteraction().getOption("time", OptionMapping::getAsInt);
		boolean revoke = false;

		int finalTime;
		if (time == null) {
			if (interaction.event().getMember().getRoles().contains(sudoRole)) {
				revoke = true;
				finalTime = 0; // Placeholder value.
			} else {
				finalTime = 30;
			}
		} else {
			finalTime = time;
		}

		if (!revoke && (finalTime < 1 || finalTime > 120)) {
			return new MessageResponse("Invalid amount of time. Must be between 1 minute and 2 hours/120 minutes.");
		}

		Member member = interaction.event().getMember();
		assert member != null;

		DatabaseAccess access = DatabaseAccess.get();

		if (revoke) {
			access.removeSudoerExpiryTime(member);
			guild.removeRoleFromMember(member, sudoRole).queue();

			if (SUDOER_TIMERS.containsKey(member)) {
				SUDOER_TIMERS.get(member).cancel(false);
				SUDOER_TIMERS.remove(member);
			}

			return new MessageResponse("You have removed the sudo role from yourself.");
		}

		guild.addRoleToMember(member, sudoRole).queue();
		access.setSudoerExpiryTime(member, finalTime);
		SUDOER_TIMERS.compute(member, (m, voidScheduledFuture) -> {
			if (voidScheduledFuture != null) {
				voidScheduledFuture.cancel(false);
			}
			return SchedulerUtil.schedule(
					() -> {
						try {
							access.removeSudoerExpiryTime(member);
							guild.removeRoleFromMember(m, sudoRole).queue();
							SUDOER_TIMERS.remove(member);
						} catch (SQLException e) {
							GardenBot.LOG.error("Failed to remove sudoer status from the database. ", e);
						}
					},
					finalTime,
					TimeUnit.MINUTES
			);
		});

		int hours = finalTime / 60;
		int minutes = finalTime % 60;

		StringBuilder timeString = new StringBuilder();

		boolean hasHours = hours > 0;
		boolean hasMinutes = minutes > 0;

		if (hasHours) {
			timeString.append(hours).append(" hours");
		}
		if (hasMinutes) {
			if (hasHours) {
				timeString.append(" and ");
			}
			timeString.append(minutes).append(" minutes");
		}

		return new MessageResponse("You have gained the sudo role. You will have it for " + timeString + ".");
	}

	@Override
	public List<Command.Choice> getAutoCompleteChoices(String focusedOption, User user, AutoCompletionGetter autoCompletionGetter) {
		List<Command.Choice> choices = new ArrayList<>();

		Guild guild = user.getJDA().getGuildById(GardenBot.GUILD_ID);
		if (guild == null) {
			return Collections.emptyList();
		}

		Member member = guild.getMember(user);
		Role sudoerRoleId = guild.getRoleById(SUDOER_ROLE_ID);
		if (member == null || !member.getRoles().contains(sudoerRoleId)) {
			return Collections.emptyList();
		}

		Collections.addAll(
				choices,
				new Command.Choice("5 mins", 5),
				new Command.Choice("10 mins", 10),
				new Command.Choice("15 mins", 15),
				new Command.Choice("30 mins", 30),
				new Command.Choice("1 hour", 60),
				new Command.Choice("2 hours", 120)
		);
		return choices;
	}

	/// Populates sudoer times on start-up.
	public static void populateSudoerTimers(Guild guild) {
		Role sudoRole = guild.getRoleById(SUDO_ROLE_ID);
		if (sudoRole == null)
			return;

		DatabaseAccess.bind().run(() -> {
			DatabaseAccess access = DatabaseAccess.get();
			try {
				Map<String, Long> expiryTimes = access.getSudoerExpiryTimes();

				for (Map.Entry<String, Long> entry : expiryTimes.entrySet()) {
					String userId = entry.getKey();
					long expiryTime = entry.getValue();

					Member member = guild.getMemberById(userId);
					if (member == null)
						continue;

					long time = expiryTime - Instant.now().toEpochMilli();
					if (time <= 0) {
						access.removeSudoerExpiryTime(member);
						guild.removeRoleFromMember(member, sudoRole).queue();
					} else {
						SUDOER_TIMERS.put(member, SchedulerUtil.schedule(
								() -> guild.removeRoleFromMember(member, sudoRole).queue(),
								time,
								TimeUnit.MILLISECONDS
						));
					}
				}
			} catch (SQLException e) {
				GardenBot.LOG.error("Failed to retrieve sudoer times on startup. ", e);
			}
		});
	}
}
