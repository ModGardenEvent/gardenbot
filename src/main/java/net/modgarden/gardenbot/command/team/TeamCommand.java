package net.modgarden.gardenbot.command.team;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.modgarden.gardenbot.GardenBot;
import net.modgarden.gardenbot.client.exception.HypertextException;
import net.modgarden.gardenbot.client.modgarden.event.GenreAndEvent;
import net.modgarden.gardenbot.client.modgarden.project.ModGardenProject;
import net.modgarden.gardenbot.client.modgarden.project.ModGardenSubmission;
import net.modgarden.gardenbot.client.modgarden.user.ModGardenUser;
import net.modgarden.gardenbot.command.GroupSlashCommand;
import net.modgarden.gardenbot.command.SlashCommand;
import net.modgarden.gardenbot.client.ModGarden;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.List;

public class TeamCommand extends GroupSlashCommand<SlashCommand> {
	private static final int ADMINISTRATOR_PERMISSION_BITS = 0x1;
	private static final int EDIT_PROJECT_PERMISSION_BITS = 0x20;

	public TeamCommand() {
		super(
			"team",
			"Modify the team of a Mod Garden project.",
				TeamInviteCommand::new,
				TeamKickCommand::new,
				TeamLeaveCommand::new
		);
	}

	protected static List<Command.Choice> getProjectAutoCompleteChoices(User user) {
		try {
			ModGardenUser modGardenUser = getModGardenUser(user);
			if (modGardenUser == null) {
				return Collections.emptyList();
			}
			List<ModGardenSubmission> activeSubmissions = getActiveSubmissions();
			return getUserProjects(modGardenUser)
					.parallelStream()
					.sorted(projectComparator(activeSubmissions))
					.map(modGardenProject -> new Command.Choice(modGardenProject.metadata.name, modGardenProject.id))
					.toList();
		} catch (Exception ignored) {
			return Collections.emptyList();
		}
	}


	protected static List<Command.Choice> getEditableProjectAutoCompleteChoices(User user) {
		try {
			ModGardenUser modGardenUser = getModGardenUser(user);
			if (modGardenUser == null) {
				return Collections.emptyList();
			}
			List<ModGardenSubmission> activeSubmissions = getActiveSubmissions();
			return getUserProjects(modGardenUser)
					.parallelStream()
					.sorted(projectComparator(activeSubmissions))
					.filter(modGardenProject -> hasPermissions(Long.parseLong(modGardenProject.permissions.getOrDefault(modGardenUser.id, "0"))))
					.map(modGardenProject -> new Command.Choice(modGardenProject.metadata.name, modGardenProject.id))
					.toList();
		} catch (Exception ignored) {
			return Collections.emptyList();
		}
	}

	@Nullable
	protected static ModGardenUser getModGardenUser(User user) throws IOException, InterruptedException {
		HttpResponse<InputStream> userStream = ModGarden.get(
				"v2/users/" + user.getId() + "?by=integration_discord",
				HttpResponse.BodyHandlers.ofInputStream()
		);
		if (userStream.statusCode() != 200) {
			return null;
		}
		JsonElement userJson = JsonParser.parseReader(new InputStreamReader(userStream.body()));
		return GardenBot.GSON.fromJson(userJson, ModGardenUser.class);
	}

	@Nullable
	protected static ModGardenProject getProject(String projectId) throws IOException, InterruptedException {
		HttpResponse<InputStream> byIdProjectStream = ModGarden.getProject(projectId);

		if (byIdProjectStream.statusCode() != 200) {
			return null;
		}
		JsonElement projectJson = JsonParser.parseReader(new InputStreamReader(byIdProjectStream.body()));
		return GardenBot.GSON.fromJson(projectJson, ModGardenProject.class);
	}

	protected static boolean hasPermissions(long userPermissions) {
		boolean hasPermissions = (EDIT_PROJECT_PERMISSION_BITS & userPermissions) > 0;
		boolean hasAdministrator = (ADMINISTRATOR_PERMISSION_BITS & userPermissions) != 0;
		return hasAdministrator || hasPermissions;
	}

	private static List<ModGardenProject> getUserProjects(ModGardenUser user) throws IOException, InterruptedException {
		return user.projects()
				.parallelStream()
				.map(s -> {
					try {
						return getProject(s);
					} catch (Exception value) {
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
