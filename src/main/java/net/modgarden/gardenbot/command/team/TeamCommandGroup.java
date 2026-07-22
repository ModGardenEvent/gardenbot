package net.modgarden.gardenbot.command.team;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.modgarden.gardenbot.GardenBot;
import net.modgarden.gardenbot.client.ModGarden;
import net.modgarden.gardenbot.client.exception.HypertextException;
import net.modgarden.gardenbot.client.mod_garden.event.GenreAndEvent;
import net.modgarden.gardenbot.client.mod_garden.project.ModGardenProject;
import net.modgarden.gardenbot.client.mod_garden.project.ModGardenSubmission;
import net.modgarden.gardenbot.client.mod_garden.user.ModGardenUser;
import net.modgarden.gardenbot.command.CommandGroup;
import net.modgarden.gardenbot.command.SlashCommand;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class TeamCommandGroup extends CommandGroup<SlashCommand> {
	private static final int ADMINISTRATOR_PERMISSION_BITS = 0x1;
	private static final int EDIT_PROJECT_PERMISSION_BITS = 0x20;

	public TeamCommandGroup() {
		super(
				"team",
				"Modify the team of a Mod Garden project.",
				TeamInviteCommand::new,
				TeamModifyCommand::new
		);
	}

	protected static List<Command.Choice> getProjectAutoCompleteChoices(User user) {
		try {
			ModGardenUser modGardenUser = ModGarden.getUserByDiscordUser(user);
			if (modGardenUser == null) {
				return Collections.emptyList();
			}
			List<ModGardenSubmission> activeSubmissions = getActiveSubmissions();
			return getUserProjects(modGardenUser)
					.parallelStream()
					.sorted(projectComparator(activeSubmissions))
					.map(modGardenProject -> new Command.Choice(modGardenProject.metadata().name(), modGardenProject.id()))
					.toList();
		} catch (Exception ignored) {
			return Collections.emptyList();
		}
	}


	protected static List<Command.Choice> getEditableProjectAutoCompleteChoices(User user) {
		try {
			ModGardenUser modGardenUser = ModGarden.getUserByDiscordUser(user);
			if (modGardenUser == null) {
				return Collections.emptyList();
			}
			List<ModGardenSubmission> activeSubmissions = getActiveSubmissions();
			return getUserProjects(modGardenUser)
					.parallelStream()
					.filter(modGardenProject -> hasPermissions(Long.parseLong(modGardenProject.permissions().getOrDefault(modGardenUser.id(), "0"))))
					.sorted(projectComparator(activeSubmissions))
					.map(modGardenProject -> new Command.Choice(modGardenProject.metadata().name(), modGardenProject.id()))
					.toList();
		} catch (Exception ignored) {
			return Collections.emptyList();
		}
	}

	protected static boolean hasPermissions(long userPermissions) {
		boolean hasPermissions = (EDIT_PROJECT_PERMISSION_BITS & userPermissions) > 0;
		boolean hasAdministrator = (ADMINISTRATOR_PERMISSION_BITS & userPermissions) != 0;
		return hasAdministrator || hasPermissions;
	}

	private static List<ModGardenProject> getUserProjects(ModGardenUser user) {
		return user.projects()
				.parallelStream()
				.map(s -> {
					try {
						return ModGarden.getProject(s);
					} catch (Exception e) {
						GardenBot.LOG.error("Failed to get project from ID '{}'.", s);
						return null;
					}
				})
				.filter(Objects::nonNull)
				.toList();
	}

	private static List<ModGardenSubmission> getActiveSubmissions() throws HypertextException {
		GenreAndEvent genreAndEvent = ModGarden.getActiveEvent();
		if (genreAndEvent == null) {
			return Collections.emptyList();
		}
		return ModGarden.getSubmissions(genreAndEvent.genre().slug(), genreAndEvent.event().slug());
	}

	private static Comparator<ModGardenProject> projectComparator(List<ModGardenSubmission> activeSubmissions) {
		return (project, otherProject) -> {
			boolean isActive = false;
			boolean otherIsActive = false;

			boolean isEmpty = project.submissions().isEmpty();
			boolean isOtherEmpty = otherProject.submissions().isEmpty();

			if (isEmpty != isOtherEmpty) {
				return Boolean.compare(isEmpty, isOtherEmpty);
			}

			for (ModGardenSubmission submission : activeSubmissions) {
				if (project.submissions().contains(submission.project().id())) {
					isActive = true;
				}
				if (otherProject.submissions().contains(submission.project().id())) {
					otherIsActive = true;
				}
			}

			if (isActive != otherIsActive) {
				return Boolean.compare(isActive, otherIsActive);
			}

			return project.metadata().name().compareTo(otherProject.metadata().name());
		};
	}
}
