package net.modgarden.gardenbot.command.submission;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.modgarden.gardenbot.GardenBot;
import net.modgarden.gardenbot.client.ModGarden;
import net.modgarden.gardenbot.client.exception.HypertextException;
import net.modgarden.gardenbot.client.mod_garden.event.ModGardenEvent;
import net.modgarden.gardenbot.client.mod_garden.project.ModGardenProject;
import net.modgarden.gardenbot.client.mod_garden.project.ModGardenSubmission;
import net.modgarden.gardenbot.client.mod_garden.user.ModGardenUser;
import net.modgarden.gardenbot.command.AutoCompletionGetter;
import net.modgarden.gardenbot.command.SlashCommand;
import net.modgarden.gardenbot.command.SlashCommandOption;
import net.modgarden.gardenbot.interaction.SlashCommandInteraction;
import net.modgarden.gardenbot.response.MessageResponse;
import net.modgarden.gardenbot.response.Response;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

// TODO: Backend V2.
public class SubmissionUnsubmitCommand extends SlashCommand {
	private static final int ADMINISTRATOR_PERMISSION_BITS = 0x1;
	private static final int EDIT_PROJECT_PERMISSION_BITS = 0x20;

	public SubmissionUnsubmitCommand() {
		super(
				"unsubmit",
				"Unsubmit a project from a current Mod Garden event.",
				new SlashCommandOption(
						OptionType.STRING,
						"project",
						"The project to unsubmit from the current event.",
						true,
						true
				)
		);
	}

	@NotNull
	@Override
	public Response respond(SlashCommandInteraction interaction) {
		interaction.event().deferReply(true).queue();
		User user = interaction.event().getUser();

		String projectArgument = interaction.event().getOption("project", OptionMapping::getAsString);
		assert projectArgument != null;

		try {
			ModGardenEvent modGardenEvent = ModGarden.getActiveEvent().event();
			if (modGardenEvent == null) {
				return new MessageResponse("No Mod Garden event is currently open for submissions.");
			}

			ModGardenUser modGardenUser = ModGarden.getUserByDiscordUser(user);
			if (modGardenUser == null) {
				return new MessageResponse("""
						You do not have a Mod Garden account.
						Please create one with **/account create**.""");
			}

			// Prioritize project name
			ModGardenProject modGardenProject = ModGarden.getProjectFromModId(projectArgument);
			if (modGardenProject == null) {
				modGardenProject = ModGarden.getProject(projectArgument);
				if (modGardenProject == null) {
					// Find project using the project's name just so copy-pasting doesn't error...
					for (String projectId : modGardenUser.projects()) {
						ModGardenProject potentialProject = ModGarden.getProject(projectId);
						if (potentialProject != null && projectArgument.equals(potentialProject.metadata().name())) {
							modGardenProject = potentialProject;
							break;
						}
					}
				}
			}

			if (modGardenProject == null) {
				return new MessageResponse("Unable to find project '" + projectArgument + "'.");
			}

			String friendlyProjectName = modGardenProject.metadata().name();

			long userPermissions = Long.parseLong(modGardenProject.permissions().getOrDefault(modGardenUser.id(), "0"));
			if (!hasPermissions(userPermissions)) {
				return new MessageResponse("You do not have permission to unsubmit the project '" + friendlyProjectName + "'.");
			}

			ModGardenSubmission modGardenSubmission = null;
			for (String submissionId : modGardenProject.submissions()) {
				ModGardenSubmission potentialSubmission = ModGarden.getSubmission(submissionId);
				if (potentialSubmission != null && potentialSubmission.eventId().equals(modGardenEvent.id())) {
					modGardenSubmission = potentialSubmission;
				}
			}

			if (modGardenSubmission == null) {
				return new MessageResponse("Project '" + friendlyProjectName + "' was never submitted to " + modGardenEvent.metadata().name() + ".");
			}

			ModGarden.deleteSubmission(modGardenSubmission.id());

			modGardenProject = ModGarden.getProject(modGardenProject.id());
			if (modGardenProject != null && modGardenProject.submissions().isEmpty()) {
				ModGarden.deleteProject(modGardenProject.id());
			}

			return new MessageResponse("Successfully unsubmitted '" + friendlyProjectName + "' from " + modGardenEvent.metadata().name() + "!");
		} catch (Exception e) {
			return exceptionResponse(e.getMessage());
		}
	}


	public List<Command.Choice> getAutoCompleteChoices(String focusedOption,
	                                                   User user,
	                                                   AutoCompletionGetter optionCompletionGetter) {
		try {
			ModGardenUser modGardenUser = ModGarden.getUserByDiscordUser(user);
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

	protected static boolean hasPermissions(long userPermissions) {
		boolean hasPermissions = (EDIT_PROJECT_PERMISSION_BITS & userPermissions) > 0;
		boolean hasAdministrator = (ADMINISTRATOR_PERMISSION_BITS & userPermissions) != 0;

		return hasAdministrator || hasPermissions;
	}
}
