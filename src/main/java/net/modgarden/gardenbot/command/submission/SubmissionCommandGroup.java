package net.modgarden.gardenbot.command.submission;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.Command;
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
import net.modgarden.gardenbot.command.CommandGroup;
import net.modgarden.gardenbot.command.SlashCommand;
import org.jetbrains.annotations.Nullable;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

public class SubmissionCommandGroup extends CommandGroup<SlashCommand> {
	public SubmissionCommandGroup() {
		super(
				"submission",
				"Commands relating to submissions to the current Mod Garden event.",
				SubmissionSubmitCommand::new,
				SubmissionUnsubmitCommand::new,
				SubmissionUpdateCommand::new
		);
	}

	protected static List<Command.Choice> getModGardenProjectChoices(User discordUser) {
		try {
			ModGardenUser modGardenUser = ModGarden.getUserByDiscordUser(discordUser);
			GenreAndEvent modGardenEvent = ModGarden.getActiveEvent();
			if (modGardenUser == null || modGardenEvent == null) {
				return Collections.emptyList();
			}

			return modGardenUser.projects()
					.parallelStream()
					.flatMap(projectId -> {
						ModGardenProject project;
						try {
							project = ModGarden.getProject(projectId);

							if (project == null) {
								return Stream.empty();
							}

							return project.submissions()
									.parallelStream()
									.map(submissionId -> {
										try {
											ModGardenSubmission submission = ModGarden.getSubmission(submissionId);
											if (submission != null && submission.eventId().equals(modGardenEvent.event().id())) {
												return new Command.Choice(project.metadata().name(), project.metadata().modId());
											}
										} catch (HypertextException e) {
											throw new RuntimeException(e);
										}
										return null;
									}).filter(Objects::nonNull);
						} catch (HypertextException e) {
							GardenBot.LOG.error("", e);
							return null;
						}
					}).filter(Objects::nonNull)
					.toList();
		} catch (HypertextException e) {
			GardenBot.LOG.error("", e);
			return Collections.emptyList();
		}
	}

	protected static List<Command.Choice> getModrinthProjectChoices(User discordUser) {
		try {
			ModGardenUser mgUser = ModGarden.getUserByDiscordUser(discordUser);
			if (mgUser == null) {
				return Collections.emptyList();
			}

			ModrinthUserIntegration modrinthIntegration = mgUser.integrations().modrinth();
			if (modrinthIntegration == null || modrinthIntegration.userId() == null) {
				return Collections.emptyList();
			}

			GenreAndEvent genreAndEvent = ModGarden.getDevelopmentTimeEvent();

			if (genreAndEvent == null || !"minecraft".equals(genreAndEvent.event().platform().game())) {
				return Collections.emptyList();
			}

			return Modrinth.getProjectsFromUser(modrinthIntegration.userId())
					.parallelStream()
					.filter(modrinthProject -> filterModrinthProjects(modrinthProject, genreAndEvent.event()))
					.sorted(SubmissionCommandGroup::sortModrinthProjects)
					.map(modrinthProject -> new Command.Choice(modrinthProject.title(), modrinthProject.slug()))
					.toList();
		} catch (Exception e) {
			GardenBot.LOG.error("", e);
			return Collections.emptyList();
		}
	}


	protected static List<Command.Choice> getModrinthVersionChoices(ModGardenSubmission submission) throws HypertextException {
		if (submission.platform().projectId() == null) {
			return Collections.emptyList();
		}

		ModrinthProject project = Modrinth.getProject(submission.platform().projectId());
		if (project == null) {
			return Collections.emptyList();
		}

		return Modrinth.getVersionsFromProject(project.id())
				.parallelStream()
				.map(version -> {
					try {
						if (Modrinth.isForMinecraftLoaderAndVersionOfEvent(version, ModGarden.getActiveEvent().event())) {
							return version;
						}
					} catch (HypertextException e) {
						GardenBot.LOG.error("", e);
					}
					return null;
				}).filter(Objects::nonNull)
				.sorted(SubmissionCommandGroup::sortModrinthVersions)
				.map(modrinthVersion -> new Command.Choice(modrinthVersion.name(), modrinthVersion.versionNumber()))
				.toList();
	}

	@Nullable
	protected static ModGardenProject getModGardenProject(ModGardenUser modGardenUser, String project) throws HypertextException {
		ModGardenProject modGardenProject = project.matches(GardenBot.SAFE_URL_REGEX)
				? ModGarden.getProjectFromModId(project)
				: null;

		if (modGardenProject == null) {
			modGardenProject = project.matches(GardenBot.SAFE_URL_REGEX)
					? ModGarden.getProject(project)
					: null;
			if (modGardenProject == null) {
				// Find project using the project's name just so copy-pasting doesn't error...
				modGardenProject = getModGardenProjectFromName(modGardenUser, project);
			}
		}

		return modGardenProject;
	}

	@Nullable
	protected static ModrinthProject getModrinthProject(ModGardenUser modGardenUser, String externalProject) throws HypertextException {
		String mappedIdOrSlug = mapFromProjectUrl(externalProject);

		ModrinthProject modrinthProject = mappedIdOrSlug.matches(GardenBot.SAFE_URL_REGEX)
				? Modrinth.getProject(mappedIdOrSlug)
				: null;

		if (modrinthProject == null) {
			// Find project using the project's name just so copy-pasting doesn't error...
			return getModrinthProjectFromName(modGardenUser, mappedIdOrSlug);
		}

		return modrinthProject;
	}

	@Nullable
	protected static ModrinthVersion getModrinthVersion(ModrinthProject project, String versionIdOrNumber) throws HypertextException {
		String mappedIdOrSlug = mapFromVersionUrl(versionIdOrNumber);

		ModrinthVersion version = mappedIdOrSlug.matches(GardenBot.SAFE_URL_REGEX)
				? Modrinth.getVersion(mappedIdOrSlug)
				: null;

		if (version == null) {
			// Find version using the version's name just so copy-pasting doesn't error...
			return getModrinthVersionFromNumberOrName(project, mappedIdOrSlug);
		}

		return version;
	}

	@Nullable
	protected static ModGardenSubmission getCurrentEventSubmission(ModGardenProject project, ModGardenEvent event) throws HypertextException {
		for (String submissionId : project.submissions()) {
			ModGardenSubmission potentialSubmission = ModGarden.getSubmission(submissionId);
			if (potentialSubmission != null && potentialSubmission.eventId().equals(event.id())) {
				return potentialSubmission;
			}
		}

		return null;
	}

	// Unless a loader comes and does some fuckshit with their name like NeoForge
	// This should work fine.
	protected static String capitalizeLoaderName(String modLoader) {
		return modLoader.substring(0, 1).toUpperCase(Locale.ROOT) + modLoader.substring(1);
	}

	@Nullable
	protected static ModrinthData getProjectAndVersion(ModGardenEvent event,
													   @Nullable ModGardenSubmission existingSubmission,
													   ModGardenUser user,
													   String urlOrExternal) throws HypertextException {
		ModrinthProject project = urlOrExternal == null
				? null
				: getModrinthProject(user, urlOrExternal);
		boolean externalIsProject;

		if (existingSubmission != null && existingSubmission.platform().type().equals("modrinth") && project == null) {
			project = getModrinthProject(user, existingSubmission.platform().projectId());
			externalIsProject = false;
		} else {
			externalIsProject = true;
		}

		if (project != null) {
			ModrinthVersion version;
			if (!externalIsProject) {
				version = getModrinthVersion(project, urlOrExternal);
			} else {
				version = Modrinth.getLatestVersionOfProjectForEvent(project.id(), event);
				if (version == null) {
					version = Modrinth.getLatestVersionOfProject(project.id());
				}
			}

			if (version != null) {
				return new ModrinthData(project, version);
			}
		}

		return null;
	}

	protected record ModrinthData(ModrinthProject project, ModrinthVersion version) {

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

	@Nullable
	private static ModrinthVersion getModrinthVersionFromNumberOrName(ModrinthProject project, String versionNumberOrName) throws HypertextException {
		for (ModrinthVersion version : Modrinth.getVersionsFromProject(project.id())) {
			if (version.versionNumber().equals(versionNumberOrName) || version.name().equals(versionNumberOrName)) {
				return version;
			}
		}

		return null;
	}

	private static String mapFromProjectUrl(String url) {
		if (url.matches("^https://modrinth\\.com/(project|mod)/([\\w!@$()`.+,\"\\-']{3,64})/version/([\\w!@$()`.+,\"\\-']{3,64})$")) {
			int indexOfSlash;
			// Remove everything after project id/slug.
			indexOfSlash = url.indexOf("/version/");
			url = url.substring(0, indexOfSlash);
			indexOfSlash = url.lastIndexOf("/");
			return url.substring(indexOfSlash + 1);
		}
		if (url.matches("^https://modrinth\\.com/(project|mod)/([\\w!@$()`.+,\"\\-']{3,64})$")) {
			int i = url.lastIndexOf("/");
			return url.substring(i + 1);
		}
		return url;
	}

	private static String mapFromVersionUrl(String url) {
		if (url.matches("^https://modrinth\\.com/(project|mod)/([\\w!@$()`.+,\"\\-']{3,64})/version/([\\w!@$()`.+,\"\\-']{3,64})$")) {
			int i = url.lastIndexOf("/");
			return url.substring(i + 1);
		}
		return url;
	}

	private static boolean filterModrinthProjects(ModrinthProject modrinthProject,
												  ModGardenEvent modGardenEvent) {
		try {
			List<ModrinthVersion> versions = Modrinth.getVersionsFromProject(modrinthProject.id());
			for (ModrinthVersion version : versions) {
				if (Modrinth.isForMinecraftLoaderAndVersionOfEvent(
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

	@Nullable
	private static ModGardenProject getModGardenProjectFromName(ModGardenUser modGardenUser, String modGardenProjectName) throws HypertextException {
		for (String projectId : modGardenUser.projects()) {
			ModGardenProject potentialProject = ModGarden.getProject(projectId);
			if (potentialProject != null && modGardenProjectName.equals(potentialProject.metadata().name())) {
				return potentialProject;
			}
		}

		return null;
	}

	private static int sortModrinthProjects(ModrinthProject project,
	                                        ModrinthProject otherProject) {
		ZonedDateTime projectUpdated = ZonedDateTime.parse(project.updated(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
		ZonedDateTime otherProjectUpdated = ZonedDateTime.parse(otherProject.updated(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);

		return -projectUpdated.compareTo(otherProjectUpdated);
	}

	private static int sortModrinthVersions(ModrinthVersion version,
											ModrinthVersion otherVersion) {
		ZonedDateTime projectUpdated = ZonedDateTime.parse(version.datePublished(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
		ZonedDateTime otherProjectUpdated = ZonedDateTime.parse(otherVersion.datePublished(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);

		return -projectUpdated.compareTo(otherProjectUpdated);
	}
}
