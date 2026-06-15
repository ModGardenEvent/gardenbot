package net.modgarden.gardenbot.command.submission;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.modgarden.gardenbot.GardenBot;
import net.modgarden.gardenbot.client.ModGarden;
import net.modgarden.gardenbot.client.Modrinth;
import net.modgarden.gardenbot.client.exception.HypertextException;
import net.modgarden.gardenbot.client.mod_garden.event.GenreAndEvent;
import net.modgarden.gardenbot.client.mod_garden.event.ModGardenEvent;
import net.modgarden.gardenbot.client.mod_garden.project.ModGardenProject;
import net.modgarden.gardenbot.client.mod_garden.project.ModGardenSubmission;
import net.modgarden.gardenbot.client.mod_garden.user.ModGardenUser;
import net.modgarden.gardenbot.client.modrinth.ModrinthProject;
import net.modgarden.gardenbot.client.modrinth.ModrinthVersion;
import net.modgarden.gardenbot.command.AutoCompletionGetter;
import net.modgarden.gardenbot.command.SlashCommand;
import net.modgarden.gardenbot.command.SlashCommandOption;
import net.modgarden.gardenbot.interaction.SlashCommandInteraction;
import net.modgarden.gardenbot.response.MessageResponse;
import net.modgarden.gardenbot.response.Response;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

import static net.modgarden.gardenbot.command.submission.SubmissionCommand.*;

public class SubmissionSubmitCommand extends SlashCommand {
	public SubmissionSubmitCommand() {
		super(
				"submit",
				"Submit your project to a current Mod Garden event.",
				new SlashCommandOption(
						OptionType.STRING,
						"platform",
						"The source platform to use to submit your project using.",
						true,
						true
				),
				new SlashCommandOption(
						OptionType.STRING,
						"url-or-external-project",
						"Either the file URL or Modrinth project to submit.",
						true,
						true
				)
				// TODO: Implement these extra bits of information...
//				new SlashCommandOption(
//						OptionType.BOOLEAN,
//						"is_primary",
//						"If specified and true, this will set the submission as the primary file for the specified project.",
//						false
//				),
//				new SlashCommandOption(
//						OptionType.STRING,
//						"mod_garden_project",
//						"If specified, the project to submit the Modrinth version to. Otherwise, a new project will be created.",
//						false,
//						true
//				)
		);
	}

	@NotNull
	@Override
	@SuppressWarnings("DuplicatedCode")
	public Response respond(SlashCommandInteraction interaction) {
		interaction.event().deferReply(true).queue();
		User user = interaction.event().getUser();

		String platform = interaction.event().getOption("platform", OptionMapping::getAsString);
		String urlOrExternalProject = interaction.event().getOption("url-or-external-project", OptionMapping::getAsString);

		assert platform != null;
		assert urlOrExternalProject != null;

		// This one's up here to allow for cleaning up later.
		ModGardenProject projectForCleanup = null;

		try {
			GenreAndEvent modGardenGenreAndEvent = ModGarden.getDevelopmentTimeEvent();
			ModGardenUser modGardenUser = ModGarden.getUserByDiscordUser(user);

			if (modGardenGenreAndEvent == null) {
				return new MessageResponse("No Mod Garden event is currently open for submissions.");
			}

			ModGardenEvent modGardenEvent = modGardenGenreAndEvent.event();

			if (modGardenUser == null) {
				return new MessageResponse("""
						You do not have a Mod Garden account.
						Please create one with **/account create**.""");
			}

			if ("download_url".equals(platform) || "Download URL".equalsIgnoreCase(platform)) {
				return new MessageResponse("Download URLs as a submission platform are not yet implemented...");
			}

			if ("modrinth".equalsIgnoreCase(platform)) {
				// TODO: Modrinth project owner validation... After account linking is updated...
				ModrinthProject modrinthProject = getModrinthProject(modGardenUser, urlOrExternalProject);

				if (modrinthProject == null) {
					return exceptionResponse("Could not find Modrinth project '" + urlOrExternalProject + "'.");
				}

				ModrinthVersion modrinthVersion = Modrinth.getLatestVersionOfProject(modrinthProject.id(), modGardenEvent);
				if (modrinthVersion == null) {
					return new MessageResponse(
							"You do not have a Modrinth project that is valid for the modloader and the game version.\nExpected %s for %s"
									.formatted(capitalizeLoaderName(modGardenEvent.platform().modLoader()), modGardenEvent.platform().gameVersion())
					);
				}

				String modId = Modrinth.getModIdFromVersion(modrinthVersion);

				for (ModGardenSubmission submission : ModGarden.getSubmissions(modGardenGenreAndEvent.genre().slug(), modGardenGenreAndEvent.event().slug())) {
					if (modId.equals(submission.project().metadata().modId())) {
						return new MessageResponse("Mod with ID '" + modId + "' has already been submitted to event '" + modGardenEvent.metadata().name() + "'."
								+ "\nPlease use **/submission update** to update your submission.");
					}
				}

				// See if the project exists already and use that if possible...
				ModGardenProject modGardenProject = ModGarden.getProjectFromModId(modId);
				if (modGardenProject == null) {
					modGardenProject = ModGarden.createProject(modrinthProject.title());
					// Save this for cleanup in the case of an exception.
					projectForCleanup = modGardenProject;

					if (modGardenProject == null) {
						throw new HypertextException(500, "Failed to create project.");
					}

					ModGarden.transferProjectOwnership(modGardenProject, modGardenUser);

					// Update the project variables with the latest information.
					modGardenProject = ModGarden.getProject(modGardenProject.id());
					projectForCleanup = modGardenProject;
				}

				if (modGardenProject == null) {
					throw new HypertextException(500, "Failed to create project.");
				}

				if (!modGardenProject.team().containsKey(modGardenUser.id())) {
					return new MessageResponse("You do not have permissions to create submissions for the specified project.");
				}

				ModGardenSubmission submission = ModGarden.createSubmissionModrinth(modGardenProject.id(), modGardenEvent.id(), modrinthProject.id(), modrinthVersion.id());
				if (submission == null) {
					throw new HypertextException(500, "Failed to create submission.");
				}

				return new MessageResponse("Successfully submitted '" + modrinthProject.title() + "' to " + modGardenEvent.metadata().name() + "!");
			}
		} catch (Exception e) {
			// If a project was created and does not have data...
			// Clean up!
			if (projectForCleanup != null && projectForCleanup.submissions().isEmpty()) {
				try {
					ModGarden.deleteProject(projectForCleanup.id());
				} catch (HypertextException ex) {
					GardenBot.LOG.error("", ex);
					return exceptionResponse(ex);
				}
			}
			GardenBot.LOG.error("", e);
			return exceptionResponse(e.getMessage());
		}
		return new MessageResponse("Invalid platform for Mod Garden '" + platform + "'.");
	}

	@Override
	public List<Command.Choice> getAutoCompleteChoices(String focusedOption, User user, AutoCompletionGetter autoCompletionGetter) {
		if ("platform".equals(focusedOption)) {
			return getPlatformChoices();
		}

		if ("modrinth".equals(autoCompletionGetter.getOption("platform", OptionMapping::getAsString)) && "url-or-external-project".equals(focusedOption)) {
			return getModrinthProjectChoices(user);
		}

		return Collections.emptyList();
	}
}
