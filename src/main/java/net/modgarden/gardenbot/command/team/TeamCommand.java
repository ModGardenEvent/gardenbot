package net.modgarden.gardenbot.command.team;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.modgarden.gardenbot.GardenBot;
import net.modgarden.gardenbot.command.GroupSlashCommand;
import net.modgarden.gardenbot.command.SlashCommand;
import net.modgarden.gardenbot.util.ModGardenAPIClient;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.*;
import java.util.List;

public class TeamCommand extends GroupSlashCommand<SlashCommand> {
	private static final int ADMINISTRATOR_PERMISSION_BITS = 0x1;
	private static final int EDIT_PROJECT_PERMISSION_BITS = 0x20;

	public TeamCommand() {
		super(
			"team",
			"Modify the team of a Mod Garden project.",
				InviteCommand::new,
				KickCommand::new,
				LeaveCommand::new
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
		HttpResponse<InputStream> userStream = ModGardenAPIClient.get(
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
		HttpResponse<InputStream> byIdProjectStream = ModGardenAPIClient.get(
				"v2/projects/" + projectId,
				HttpResponse.BodyHandlers.ofInputStream()
		);
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
		return user.projects
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

	@Nullable
	private static ModGardenEvent getActiveEvent() throws IOException, InterruptedException {
		// TODO: Make an Active Events Endpoint
		HttpResponse<InputStream> eventsStream = ModGardenAPIClient.get(
				"v2/events/mod-garden",
				HttpResponse.BodyHandlers.ofInputStream()
		);
		if (eventsStream.statusCode() != 200) {
			return null;
		}

		JsonElement eventsJson = JsonParser.parseReader(new InputStreamReader(eventsStream.body()));
		for (JsonElement element : eventsJson.getAsJsonArray()) {
			ModGardenEvent event = GardenBot.GSON.fromJson(element, ModGardenEvent.class);

			long now = Instant.now().toEpochMilli();

			long registrationOpen = Long.parseLong(event.times.registrationOpen);
			long packFreeze = Long.parseLong(event.times.packFreeze);

			if (now >= registrationOpen && now < packFreeze) {
				return event;
			}
		}

		return null;
	}

	private static List<ModGardenSubmission> getActiveSubmissions() throws IOException, InterruptedException {
		ModGardenEvent event = getActiveEvent();
		if (event == null) {
			return Collections.emptyList();
		}
		return getSubmissions("mod-garden", event.slug);
	}

	private static List<ModGardenSubmission> getSubmissions(String genreSlug, String eventSlug) throws IOException, InterruptedException {
		HttpResponse<InputStream> submissionsStream = ModGardenAPIClient.get(
				"v2/events/%s/%s/submissions"
						.formatted(genreSlug, eventSlug),
				HttpResponse.BodyHandlers.ofInputStream()
		);
		if (submissionsStream.statusCode() != 200) {
			return Collections.emptyList();
		}
		List<ModGardenSubmission> submissions = new ArrayList<>();
		JsonElement submissionsJson = JsonParser.parseReader(new InputStreamReader(submissionsStream.body()));

		for (JsonElement element : submissionsJson.getAsJsonArray()) {
			ModGardenSubmission submission = GardenBot.GSON.fromJson(element, ModGardenSubmission.class);
			submissions.add(submission);
		}

		return submissions;
	}

	private static Comparator<ModGardenProject> projectComparator(List<ModGardenSubmission> activeSubmissions) {
		return (project, otherProject) -> {
			boolean isActive = false;
			boolean otherIsActive = false;

			for (ModGardenSubmission submission : activeSubmissions) {
				if (project.submissions.contains(submission.project.id)) {
					isActive = true;
				}
				if (otherProject.submissions.contains(submission.project.id)) {
					otherIsActive = true;
				}
			}

			if (isActive != otherIsActive) {
				return Boolean.compare(isActive, otherIsActive);
			}

			boolean isEmpty = project.submissions.isEmpty();
			boolean isOtherEmpty = otherProject.submissions.isEmpty();

			if (isEmpty != isOtherEmpty) {
				return Boolean.compare(isEmpty, isOtherEmpty);
			}

			return project.metadata.name.compareTo(otherProject.metadata.name);
		};
	}

	protected static class ModGardenUser {
		public String id;
		public List<String> projects;
	}

	protected static class ModGardenProject {
		public String id;
		public ProjectMetadata metadata;
		public Map<String, String> permissions;
		public List<String> submissions;
	}

	protected static class ProjectMetadata {
		public String name;
	}

	private static class ModGardenEvent {
		public String slug;
		public EventTimes times;
	}

	private static class ModGardenSubmission {
		public ModGardenProject project;
	}

	private static class EventTimes {
		@SerializedName("registration_open")
		public String registrationOpen;

		@SerializedName("pack_freeze")
		public String packFreeze;
	}
}
