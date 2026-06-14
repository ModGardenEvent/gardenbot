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
import net.modgarden.gardenbot.client.mod_garden.user.integration.ModrinthUserIntegration;
import net.modgarden.gardenbot.client.modrinth.ModrinthProject;
import net.modgarden.gardenbot.client.modrinth.ModrinthVersion;
import net.modgarden.gardenbot.command.AutoCompletionGetter;
import net.modgarden.gardenbot.command.SlashCommand;
import net.modgarden.gardenbot.command.SlashCommandOption;
import net.modgarden.gardenbot.interaction.SlashCommandInteraction;
import net.modgarden.gardenbot.response.MessageResponse;
import net.modgarden.gardenbot.response.Response;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class SubmissionSubmitCommand extends SlashCommand {
	public SubmissionSubmitCommand() {
		super(
				"submit",
				"Submit your project to a current Mod Garden event.",
				new SlashCommandOption(
						OptionType.STRING,
						"platform",
						"The platform to use to submit your project under.",
						true,
						true
				),
				new SlashCommandOption(
						OptionType.STRING,
						"url-or-external-project",
						"Either the file URL or Modrinth ID/slug of the project to submit.",
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
	public Response respond(SlashCommandInteraction interaction) {
		interaction.event().deferReply(true).queue();
		User user = interaction.event().getUser();

		String platform = interaction.event().getOption("platform", OptionMapping::getAsString);
		String urlOrProject = interaction.event().getOption("url-or-external-project", OptionMapping::getAsString);

		assert platform != null;
		assert urlOrProject != null;

		// This one's up here to allow for cleaning up later.
		ModGardenProject projectForCleanup = null;

		try {
			GenreAndEvent modGardenGenreAndEvent = ModGarden.getDevelopmentTimeEvent();
			ModGardenUser modGardenUser = ModGarden.getUserByDiscordUser(user);

			if (modGardenGenreAndEvent == null) {
				return new MessageResponse("No Mod Garden event is currently open for new submissions.");
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
				ModrinthProject modrinthProject = urlOrProject.matches(GardenBot.SAFE_URL_REGEX)
						? Modrinth.getProject(urlOrProject)
						: null;

				if (modrinthProject == null) {
					// Find project using the project's name just so copy-pasting doesn't error...
					modrinthProject = getModrinthProjectFromName(modGardenUser, urlOrProject);

					// If it's null after that point... Then we shall return the exception!
					if (modrinthProject == null) {
						return exceptionResponse("Could not find Modrinth project '" + urlOrProject + "'.");
					}
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
					// Save this for clean-up in the case of an exception.
					projectForCleanup = modGardenProject;

					if (modGardenProject == null) {
						throw new HypertextException(500, "Failed to create project.");
					}

					ModGarden.transferProjectOwnership(modGardenProject, modGardenUser);
				}

				if (!modGardenProject.team().containsKey(modGardenUser.id())) {
					return new MessageResponse("You do not have permissions to create submissions for the specified project.");
				}

				ModGardenSubmission submission = ModGarden.createModrinthSubmission(modGardenProject, modGardenEvent, modrinthProject, modrinthVersion);
				if (submission == null) {
					throw new HypertextException(500, "Failed to create submission.");
				}

				return new MessageResponse("Successfully submitted your Modrinth project '" + modrinthProject.title() + "' to " + modGardenEvent.metadata().name() + "!");
			}
		} catch (Exception e) {
			// If a project was created and does not have data...
			// Clean up!
			if (projectForCleanup != null && projectForCleanup.submissions().isEmpty()) {
				try {
					ModGarden.deleteProject(projectForCleanup);
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
			return List.of(
					new Command.Choice("Modrinth", "modrinth"),
					new Command.Choice("Download URL", "download_url")
			);
		}

		if (
				"modrinth".equals(autoCompletionGetter.getOption("platform", OptionMapping::getAsString))
						&& "url_or_project".equals(focusedOption)
		) {
			return getModrinthProjectCompleteChoices(user);
		}

		return Collections.emptyList();
	}

	@Nullable
	private static ModrinthProject getModrinthProjectFromName(ModGardenUser modGardenUser, String modrinthProjectName) throws HypertextException {
		ModrinthUserIntegration modrinthIntegration = modGardenUser.integrations().modrinth();
		if (modrinthIntegration == null || modrinthIntegration.userId() == null) {
			return null;
		}

		List<ModrinthProject> projects = Modrinth.getProjectsFromUser(modGardenUser.integrations().modrinth().userId())
				.stream()
				.filter(modrinthProject -> modrinthProject.title().equals(modrinthProjectName))
				.toList();

		if (projects.isEmpty()) {
			return null;
		}

		// I doubt anybody in their right mind would name two projects they own the same thing...
		if (projects.size() > 1) {
			throw new HypertextException(500, """
					Congratulations! You found the secret message by having two mods named the exact same thing...
					Maybe don't do that? Maybe just use the slug at this point? I'm not the judge of what you do...

					Feel free to tell us about Tiny Pineapple and their crimes against the Garden.""");
		}

		return projects.getFirst();
	}

	private static List<Command.Choice> getModrinthProjectCompleteChoices(User discordUser) {
		try {
			ModGardenUser mgUser = ModGarden.getUserByDiscordUser(discordUser);
			if (mgUser == null) {
				return Collections.emptyList();
			}

			ModrinthUserIntegration modrinthIntegration = mgUser.integrations().modrinth();
			if (modrinthIntegration == null || modrinthIntegration.userId() == null) {
				return Collections.emptyList();
			}

			ModGardenEvent event = ModGarden.getDevelopmentTimeEvent().event();

			if (event == null || !"minecraft".equals(event.platform().game())) {
				return Collections.emptyList();
			}

			return Modrinth.getProjectsFromUser(modrinthIntegration.userId())
					.parallelStream()
					.filter(modrinthProject -> filterModrinthProjects(modrinthProject, event))
					.sorted(SubmissionSubmitCommand::sortModrinthVersions)
					.map(modrinthProject -> new Command.Choice(modrinthProject.title(), modrinthProject.slug()))
					.toList();
		} catch (Exception e) {
			GardenBot.LOG.error("", e);
			return Collections.emptyList();
		}
	}

	private static boolean filterModrinthProjects(ModrinthProject modrinthProject,
	                                              ModGardenEvent modGardenEvent) {
		try {
			List<ModrinthVersion> versions = Modrinth.getVersionsFromProject(modrinthProject.id());
			for (ModrinthVersion version : versions) {
				if (Modrinth.forMinecraftLoaderAndVersionOfEvent(
						version,
						modGardenEvent
				)) {
					return true;
				}
			}
		} catch (Exception e) {
			GardenBot.LOG.error("", e);
		}
		return false;
	}

	private static int sortModrinthVersions(ModrinthProject project,
	                                        ModrinthProject otherProject) {
		ZonedDateTime projectUpdated = ZonedDateTime.parse(project.updated(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
		ZonedDateTime otherProjectUpdated = ZonedDateTime.parse(otherProject.updated(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);

		return projectUpdated.compareTo(otherProjectUpdated);
	}

	// Unless a loader comes and does some fuckshit with their name like NeoForge
	// This should work fine.
	private static String capitalizeLoaderName(String modLoader) {
		return modLoader.substring(0, 1).toUpperCase(Locale.ROOT) + modLoader.substring(1);
	}
}
