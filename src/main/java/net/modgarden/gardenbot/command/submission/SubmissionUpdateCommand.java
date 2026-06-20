package net.modgarden.gardenbot.command.submission;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.modgarden.gardenbot.GardenBot;
import net.modgarden.gardenbot.client.ModGarden;
import net.modgarden.gardenbot.client.Modrinth;
import net.modgarden.gardenbot.client.exception.HypertextException;
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

import static net.modgarden.gardenbot.command.submission.SubmissionCommandGroup.*;

public class SubmissionUpdateCommand extends SlashCommand {
	public SubmissionUpdateCommand() {
		super(
				"update",
				"Update a project's version within a current Mod Garden event.",
				new SlashCommandOption(
						OptionType.STRING,
						"project",
						"The project to update.",
						true,
						true
				),
				new SlashCommandOption(
						OptionType.STRING,
						"url-or-version",
						"Either the file URL or Modrinth version of the project to submit.",
						false,
						true
				)
		);
	}

	@NotNull
	@Override
	@SuppressWarnings("DuplicatedCode")
	public Response respond(SlashCommandInteraction interaction) {
		interaction.event().deferReply(true).queue();
		User user = interaction.event().getUser();

		String project = interaction.event().getOption("project", OptionMapping::getAsString);
		String platform = interaction.event().getOption("platform", OptionMapping::getAsString);
		String urlOrVersion = interaction.event().getOption("url-or-version", OptionMapping::getAsString);

		assert project != null;
		assert platform != null;

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

			ModGardenProject modGardenProject = getModGardenProject(modGardenUser, project);
			if (modGardenProject == null) {
				return new MessageResponse("Unable to find project '" + project + "'.");
			}

			String friendlyProjectName = modGardenProject.metadata().name();

			ModGardenSubmission modGardenSubmission = getCurrentEventSubmission(modGardenProject, modGardenEvent);
			if (modGardenSubmission == null) {
				return new MessageResponse("Project '" + friendlyProjectName + "' was never submitted to " + modGardenEvent.metadata().name() + ".");
			}

			if (modGardenSubmission.platform().projectId() == null) {
				return new MessageResponse("Submission is not a Modrinth submission.");
			}

			// TODO: Modrinth project owner validation... After account linking is updated...
			ModrinthProject modrinthProject = getModrinthProject(modGardenUser, modGardenSubmission.platform().projectId());

			if (modrinthProject == null) {
				return exceptionResponse("Could not find Modrinth project associated with submission.");
			}

			ModrinthVersion modrinthVersion = urlOrVersion != null
					? getModrinthVersion(modrinthProject, urlOrVersion)
					: Modrinth.getLatestVersionOfProject(modrinthProject.id(), modGardenEvent);
			if (modrinthVersion == null) {
				return new MessageResponse(
						"Your Modrinth %s version for the modloader and the game version.\nExpected %s for %s"
								.formatted(
										urlOrVersion != null ? "version is not a valid" : "project does not have a valid",
										capitalizeLoaderName(modGardenEvent.platform().modLoader()),
										modGardenEvent.platform().gameVersion()
								)
				);
			}

			if (modrinthVersion.id().equals(modGardenSubmission.platform().versionId())) {
				return new MessageResponse(
						urlOrVersion == null
								? "Your Mod Garden project is up to date."
								: "Your Mod Garden project is already using the specified version."
				);
			}

			ModGarden.updateSubmissionModrinth(modGardenSubmission.id(), modrinthProject.id(), modrinthVersion.id());

			// Do this in the case that the project metadata has changed.
			ModGardenProject finalModGardenProject = ModGarden.getProject(modGardenProject.id());
		if (finalModGardenProject == null) {
				throw new HypertextException(500, "Could not retrieve project after updating the submission.");
			}
			String finalFriendlyProjectName = finalModGardenProject.metadata().name();

			return new MessageResponse("Successfully updated '" + finalFriendlyProjectName + "' to version '" + modrinthVersion.name() + "' for " + modGardenEvent.metadata().name() + "!");
		} catch (Exception e) {
			return exceptionResponse(e.getMessage());
		}
	}

	@Override
	public List<Command.Choice> getAutoCompleteChoices(String focusedOption, User user, AutoCompletionGetter autoCompletionGetter) {
		if ("project".equals(focusedOption)) {
			return getModGardenProjectChoices(user);
		}

		if ("url-or-version".equals(focusedOption)) {
			try {
				ModGardenProject project = getModGardenProject(ModGarden.getUserByDiscordUser(user), autoCompletionGetter.getOption("project", OptionMapping::getAsString));

				if (project == null) {
					return Collections.emptyList();
				}

				ModGardenSubmission submission = getCurrentEventSubmission(project, ModGarden.getActiveEvent().event());

				if (submission == null) {
					return Collections.emptyList();
				}

				if (submission.platform().type().equals("modrinth")) {
					return getModrinthVersionChoices(submission);
				}
			} catch (HypertextException e) {
				GardenBot.LOG.error("", e);
			}
		}

		return Collections.emptyList();
	}
}
