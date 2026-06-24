package net.modgarden.gardenbot.command.admin.event;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.modgarden.gardenbot.client.ModGarden;
import net.modgarden.gardenbot.client.exception.HypertextException;
import net.modgarden.gardenbot.client.exception.NotFoundException;
import net.modgarden.gardenbot.client.mod_garden.event.ModGardenEvent;
import net.modgarden.gardenbot.client.mod_garden.event.ModGardenGenre;
import net.modgarden.gardenbot.command.AutoCompletionGetter;
import net.modgarden.gardenbot.command.SlashCommandOption;
import net.modgarden.gardenbot.command.admin.AdminSlashCommand;
import net.modgarden.gardenbot.interaction.SlashCommandInteraction;
import net.modgarden.gardenbot.response.MessageResponse;
import net.modgarden.gardenbot.response.Response;
import net.modgarden.gardenbot.util.NullableWrapper;
import org.jetbrains.annotations.NotNull;

public class AdminRemoveEventRoleCommand extends AdminSlashCommand {
	public AdminRemoveEventRoleCommand() {
		super(
				"remove_role",
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
						"role-key",
						"The role's snake case key.",
						true,
						true
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

		String roleKey = Objects.requireNonNull(interaction.event().getOption("role-key", OptionMapping::getAsString));

		ModGarden.modifyEvent(
				genre.id(),
				event.id(),
				null,
				null,
				null,
				Map.of(roleKey, NullableWrapper.empty())
		);

		return new MessageResponse("Successfully modified event '" + event.metadata().name() + "' within genre '" + genre.metadata().name() + "'.");
	}

	@Override
	public List<Command.Choice> getAutoCompleteChoices(String focusedOption, User user, AutoCompletionGetter autoCompletionGetter) {
		// TODO: This...
		return Collections.emptyList();
	}
}
