package net.modgarden.gardenbot.command.submission;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.modgarden.gardenbot.GardenBot;
import net.modgarden.gardenbot.client.ModGarden;
import net.modgarden.gardenbot.client.Modrinth;
import net.modgarden.gardenbot.client.exception.HypertextException;
import net.modgarden.gardenbot.client.mod_garden.event.ModGardenEvent;
import net.modgarden.gardenbot.client.mod_garden.project.ModGardenProject;
import net.modgarden.gardenbot.client.mod_garden.project.ModGardenSubmission;
import net.modgarden.gardenbot.client.mod_garden.user.ModGardenUser;
import net.modgarden.gardenbot.client.mod_garden.user.integration.ModrinthUserIntegration;
import net.modgarden.gardenbot.client.modrinth.ModrinthProject;
import net.modgarden.gardenbot.client.modrinth.ModrinthVersion;
import net.modgarden.gardenbot.command.GroupSlashCommand;
import net.modgarden.gardenbot.command.SlashCommand;
import org.jetbrains.annotations.Nullable;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

public class SubmissionCommand extends GroupSlashCommand<SlashCommand> {
	public SubmissionCommand() {
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
			ModGardenEvent modGardenEvent = ModGarden.getActiveEvent().event();
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
											if (submission != null && submission.eventId().equals(modGardenEvent.id())) {
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

	protected static List<Command.Choice> getPlatformChoices() {
		return List.of(
				new Command.Choice("Modrinth", "modrinth"),
				new Command.Choice("Download URL", "download_url")
		);
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

			ModGardenEvent event = ModGarden.getDevelopmentTimeEvent().event();

			if (event == null || !"minecraft".equals(event.platform().game())) {
				return Collections.emptyList();
			}

			return Modrinth.getProjectsFromUser(modrinthIntegration.userId())
					.parallelStream()
					.filter(modrinthProject -> filterModrinthProjects(modrinthProject, event))
					.sorted(SubmissionCommand::sortModrinthProjects)
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
						if (Modrinth.forMinecraftLoaderAndVersionOfEvent(version, ModGarden.getActiveEvent().event())) {
							return version;
						}
					} catch (HypertextException e) {
						GardenBot.LOG.error("", e);
					}
					return null;
				}).filter(Objects::nonNull)
				.sorted(SubmissionCommand::sortModrinthVersions)
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
		ModrinthProject modrinthProject = externalProject.matches(GardenBot.SAFE_URL_REGEX)
				? Modrinth.getProject(externalProject)
				: null;

		if (modrinthProject == null) {
			// Find project using the project's name just so copy-pasting doesn't error...
			return getModrinthProjectFromName(modGardenUser, externalProject);
		}

		return modrinthProject;
	}

	@Nullable
	protected static ModrinthVersion getModrinthVersion(ModrinthProject project, String versionIdOrNumber) throws HypertextException {
		ModrinthVersion version = versionIdOrNumber.matches(GardenBot.SAFE_URL_REGEX)
				? Modrinth.getVersion(versionIdOrNumber)
				: null;

		if (version == null) {
			return getModrinthVersionFromNumberOrName(project, versionIdOrNumber);
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
