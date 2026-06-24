package net.modgarden.gardenbot.command.admin.event;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.modgarden.gardenbot.client.ModGarden;
import net.modgarden.gardenbot.client.exception.BadRequestException;
import net.modgarden.gardenbot.client.exception.HypertextException;
import net.modgarden.gardenbot.client.exception.NotFoundException;
import net.modgarden.gardenbot.client.mod_garden.event.EventMetadata;
import net.modgarden.gardenbot.client.mod_garden.event.EventPlatform;
import net.modgarden.gardenbot.client.mod_garden.event.EventTimes;
import net.modgarden.gardenbot.client.mod_garden.event.ModGardenEvent;
import net.modgarden.gardenbot.client.mod_garden.event.ModGardenGenre;
import net.modgarden.gardenbot.client.mod_garden.role.ModGardenRole;
import net.modgarden.gardenbot.command.AutoCompletionGetter;
import net.modgarden.gardenbot.command.SlashCommandOption;
import net.modgarden.gardenbot.command.admin.AdminSlashCommand;
import net.modgarden.gardenbot.interaction.SlashCommandInteraction;
import net.modgarden.gardenbot.response.MessageResponse;
import net.modgarden.gardenbot.response.Response;
import net.modgarden.gardenbot.util.NullableWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AdminModifyEventCommand extends AdminEventSlashCommand {
	public AdminModifyEventCommand() {
		super(
				"modify",
				"Creates an event and links a role Discord role to the event.",
				new SlashCommandOption(
						OptionType.STRING,
						"genre",
						"A genre slug representing the genre of the event.",
						true,
						true
				),
				new SlashCommandOption(
						OptionType.STRING,
						"event",
						"The event slug.",
						true,
						true
				),
				new SlashCommandOption(
						OptionType.STRING,
						"name",
						"The name of the event.",
						false,
						false
				),
				new SlashCommandOption(
						OptionType.STRING,
						"minecraft-version",
						"The Minecraft version of the event.",
						false,
						false
				),
				new SlashCommandOption(
						OptionType.ROLE,
						"participant-role",
						"The participant role to link to this event.",
						false,
						false
				),
				new SlashCommandOption(
						OptionType.STRING,
						"registration-open",
						"The time that registration will open in ISO format.",
						false,
						false
				),
				new SlashCommandOption(
						OptionType.STRING,
						"registration-close",
						"The time that registration will close in ISO format.",
						false,
						false
				),
				new SlashCommandOption(
						OptionType.STRING,
						"development-start",
						"The time that development will start in ISO format.",
						false,
						false
				),
				new SlashCommandOption(
						OptionType.STRING,
						"development-end",
							"The time that development wil end in ISO format.",
						false,
						false
				),
				new SlashCommandOption(
						OptionType.STRING,
						"pack-freeze",
						"The pack freeze time in ISO format.",
						false,
						false
				),
				new SlashCommandOption(
						OptionType.STRING,
						"description",
						"The description of the event.",
						false,
						false
				),
				new SlashCommandOption(
						OptionType.BOOLEAN,
						"remove-description",
						"Whether to remove the description.",
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
		String eventSlug = interaction.event().getOption("event", OptionMapping::getAsString);

		assert genreSlug != null;
		assert eventSlug != null;

		ModGardenGenre genre = ModGarden.getGenre(genreSlug);
		if (genre == null) {
			return new MessageResponse("No genre exists with the slug '" + genreSlug + "'.");
		}

		ModGardenEvent event = ModGarden.getEventBySlugs(genreSlug, eventSlug);

		if (event == null) {
			throw new NotFoundException("No event exists with the slug '" + eventSlug + "'.");
		}

		String name = interaction.event().getOption("name", OptionMapping::getAsString);
		String description = interaction.event().getOption("description", OptionMapping::getAsString);
		String minecraftVersion = interaction.event().getOption("minecraft-version", OptionMapping::getAsString);
		boolean removeDescription = interaction.event().getOption("remove-description", false, OptionMapping::getAsBoolean);

		assert name != null;
		assert minecraftVersion != null;

		Role participantRole = interaction.event().getOption("participant-role", OptionMapping::getAsRole);
		assert participantRole != null;

		String registrationOpen = parseTime(interaction.event().getOption("registration-open", OptionMapping::getAsString));
		String registrationClose = parseTime(interaction.event().getOption("registration-close", OptionMapping::getAsString));
		String developmentStart = parseTime(interaction.event().getOption("development-start", OptionMapping::getAsString));
		String developmentEnd = parseTime(interaction.event().getOption("development-end", OptionMapping::getAsString));
		String packFreeze = parseTime(interaction.event().getOption("pack-freeze", OptionMapping::getAsString));

		if (ModGarden.getEventBySlugs(genre.slug(), eventSlug) == null) {
			throw new BadRequestException("Event with slug '" + eventSlug + "' does not exist");
		}

		ModGarden.modifyEvent(
				genre.id(),
				event.id(),
				new EventMetadata.Modifiable(
						name,
						removeDescription ? NullableWrapper.empty() : NullableWrapper.of(description)
				),
				new EventTimes.Modifiable(
						registrationOpen,
						registrationClose,
						developmentStart,
						developmentEnd,
						packFreeze
				),
				new EventPlatform.Modifiable(
						event.platform().game(),
						null,
						minecraftVersion
				),
				Collections.emptyMap()
		);

		return new MessageResponse("Successfully modified event '" + event.metadata().name() + "' within genre '" + genre.metadata().name() + "'.");
	}

	@Override
	public List<Command.Choice> getAutoCompleteChoices(String focusedOption, User user, AutoCompletionGetter autoCompletionGetter) {
		// TODO: This...
		return Collections.emptyList();
	}

	@Nullable
	private static String parseTime(@Nullable String time) throws BadRequestException {
		try {
			if (time == null) {
				return null;
			}

			return Long.toString(
					OffsetDateTime.parse(time)
							.toInstant()
							.toEpochMilli()
			);
		} catch (DateTimeParseException e) {
			throw new BadRequestException("Invalid registration open time. Field must follow the ISO 8601 datetime format.");
		}
	}
}
