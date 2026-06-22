package net.modgarden.gardenbot.command.admin.event;

import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.modgarden.gardenbot.client.ModGarden;
import net.modgarden.gardenbot.client.exception.HypertextException;
import net.modgarden.gardenbot.client.mod_garden.event.*;
import net.modgarden.gardenbot.client.mod_garden.role.ModGardenRole;
import net.modgarden.gardenbot.command.AutoCompletionGetter;
import net.modgarden.gardenbot.command.SlashCommandOption;
import net.modgarden.gardenbot.command.admin.AdminSlashCommand;
import net.modgarden.gardenbot.interaction.SlashCommandInteraction;
import net.modgarden.gardenbot.response.MessageResponse;
import net.modgarden.gardenbot.response.Response;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AdminEventCreateCommand extends AdminSlashCommand {
	public AdminEventCreateCommand() {
		super(
				"create",
				"Creates an event and links a role Discord role to the event.",
				new SlashCommandOption(
						OptionType.STRING,
						"genre",
						"A genre slug representing the genre for the created event.",
						true,
						true
				),
				new SlashCommandOption(
						OptionType.STRING,
						"slug",
						"The slug of the event.",
						true,
						false
				),
				new SlashCommandOption(
						OptionType.STRING,
						"name",
						"The name of the event.",
						true,
						false
				),
				new SlashCommandOption(
						OptionType.STRING,
						"minecraft-version",
						"The Minecraft version of the event.",
						true,
						false
				),
				new SlashCommandOption(
						OptionType.ROLE,
						"participant-role",
						"The participant role to link to this event.",
						true,
						false
				),
				new SlashCommandOption(
						OptionType.STRING,
						"registration-open",
						"The time that registration will open in ISO format.",
						true,
						false
				),
				new SlashCommandOption(
						OptionType.STRING,
						"registration-close",
						"The time that registration will close in ISO format.",
						true,
						false
				),
				new SlashCommandOption(
						OptionType.STRING,
						"development-start",
						"The time that development will start in ISO format.",
						true,
						false
				),
				new SlashCommandOption(
						OptionType.STRING,
						"development-end",
							"The time that development wil end in ISO format.",
						true,
						false
				),
				new SlashCommandOption(
						OptionType.STRING,
						"pack-freeze",
						"The pack freeze time in ISO format.",
						true,
						false
				),
				new SlashCommandOption(
						OptionType.STRING,
						"description",
						"The description of the event.",
						false,
						false
				)
		);
	}

	@NotNull
	@Override
	public Response respond(SlashCommandInteraction interaction) throws HypertextException, SQLException {
		interaction.event().deferReply(false).queue();

		String genreSlug = interaction.event().getOption("genre", OptionMapping::getAsString);
		String slug = interaction.event().getOption("slug", OptionMapping::getAsString);

		assert genreSlug != null;
		assert slug != null;

		ModGardenGenre genre = ModGarden.getGenre(genreSlug);
		if (genre == null) {
			return new MessageResponse("No genre exists with the slug '" + genreSlug + "'.");
		}

		String name = interaction.event().getOption("name", OptionMapping::getAsString);
		String description = interaction.event().getOption("description", OptionMapping::getAsString);
		String minecraftVersion = interaction.event().getOption("minecraft-version", OptionMapping::getAsString);

		assert name != null;
		assert minecraftVersion != null;

		Role participantRole = interaction.event().getOption("participant-role", OptionMapping::getAsRole);
		assert participantRole != null;

		String registrationOpen = parseTime(interaction.event().getOption("registration-open", OptionMapping::getAsString));
		String registrationClose = parseTime(interaction.event().getOption("registration-close", OptionMapping::getAsString));
		String developmentStart = parseTime(interaction.event().getOption("development-start", OptionMapping::getAsString));
		String developmentEnd = parseTime(interaction.event().getOption("development-end", OptionMapping::getAsString));
		String packFreeze = parseTime(interaction.event().getOption("pack-freeze", OptionMapping::getAsString));

		if (registrationOpen == null) {
			return new MessageResponse("Invalid registration open time. Field must follow the ISO 8601 datetime format.");
		}

		if (registrationClose == null) {
			return new MessageResponse("Invalid registration close time. Field must follow the ISO 8601 datetime format.");
		}

		if (developmentStart == null) {
			return new MessageResponse("Invalid development start time. Field must follow the ISO 8601 datetime format.");
		}

		if (developmentEnd == null) {
			return new MessageResponse("Invalid development ehd time. Field must follow the ISO 8601 datetime format.");
		}

		if (packFreeze == null) {
			return new MessageResponse("Invalid pack freeze time. Field must follow the ISO 8601 datetime format.");
		}

		if (ModGarden.getEventBySlugs(genre.slug(), slug) != null) {
			return new MessageResponse("Event with slug '" + slug + "' already exists within the genre '" + genre.metadata().name() + "'.");
		}

		ModGardenEvent event = ModGarden.createEvent(
				genre.id(),
				slug,
				new EventMetadata(
						name,
						description
				),
				new EventTimes(
						registrationOpen,
						registrationClose,
						developmentStart,
						developmentEnd,
						packFreeze
				),
				// This can be hardcoded for now...
				new EventPlatform(
						"minecraft",
						"fabric",
						minecraftVersion
				)
		);

		ModGardenRole role = ModGarden.createUserRoleFromDiscordRole(participantRole);
		ModGarden.addRolesToEvent(genre.id(), event.id(), Map.of("participant", role.id()));

		return new MessageResponse("Successfully created event '" + name + "' within genre '" + genre.metadata().name() + "'.");
	}

	@Override
	public List<Command.Choice> getAutoCompleteChoices(String focusedOption, User user, AutoCompletionGetter autoCompletionGetter) {
		// TODO: This...
		return Collections.emptyList();
	}

	@Nullable
	private static String parseTime(String time) {
		try {
			return Long.toString(
					OffsetDateTime.parse(time)
							.toInstant()
							.toEpochMilli()
			);
		} catch (DateTimeParseException e) {
			return null;
		}
	}
}
