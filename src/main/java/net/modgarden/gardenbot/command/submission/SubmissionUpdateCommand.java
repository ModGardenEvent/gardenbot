package net.modgarden.gardenbot.command.submission;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.modgarden.gardenbot.GardenBot;
import net.modgarden.gardenbot.client.ModGarden;
import net.modgarden.gardenbot.client.Modrinth;
import net.modgarden.gardenbot.client.exception.HypertextException;
import net.modgarden.gardenbot.client.exception.InternalServerException;
import net.modgarden.gardenbot.client.mod_garden.event.ModGardenEvent;
import net.modgarden.gardenbot.client.mod_garden.project.ModGardenProject;
import net.modgarden.gardenbot.client.mod_garden.project.ModGardenSubmission;
import net.modgarden.gardenbot.client.mod_garden.project.SubmissionPlatform;
import net.modgarden.gardenbot.client.mod_garden.user.ModGardenUser;
import net.modgarden.gardenbot.command.AutoCompletionGetter;
import net.modgarden.gardenbot.command.SlashCommand;
import net.modgarden.gardenbot.command.SlashCommandOption;
import net.modgarden.gardenbot.interaction.SlashCommandInteraction;
import net.modgarden.gardenbot.response.MessageResponse;
import net.modgarden.gardenbot.response.Response;
import net.modgarden.gardenbot.util.FileUtils;
import net.modgarden.gardenbot.util.loader.FabricModJson;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

import static net.modgarden.gardenbot.command.submission.SubmissionCommandGroup.*;

public class SubmissionUpdateCommand extends SlashCommand {
	private static final String URL_REGEX = "^https://[\\w!@$()`.+,_\"-/]*.[\\w!@$()`.+,_\"-]*$";

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
						"url-or-external",
						"Either the file URL or Modrinth project/version of the project to submit.",
						false,
						true
				)
		);
	}

	@NotNull
	@Override
	@SuppressWarnings("DuplicatedCode")
	public Response respond(SlashCommandInteraction interaction) throws HypertextException {
		interaction.event().deferReply(true).queue();
		User user = interaction.event().getUser();

		String project = interaction.event().getOption("project", OptionMapping::getAsString);
		String urlProjectOrVersion = interaction.event().getOption("url-or-external", OptionMapping::getAsString);

		assert project != null;

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

		ModrinthData modrinth = getProjectAndVersion(modGardenEvent, modGardenSubmission, modGardenUser, urlProjectOrVersion);
		String version;

		if (modrinth != null) {
			if (!Modrinth.isForMinecraftLoaderAndVersionOfEvent(modrinth.version(), modGardenEvent)) {
				return new MessageResponse(
						"Your Modrinth %s version for the modloader and the game version.\nExpected %s for %s"
								.formatted(
										urlProjectOrVersion != null ? "version is not a valid" : "project does not have a valid",
										capitalizeLoaderName(modGardenEvent.platform().modLoader()),
										modGardenEvent.platform().gameVersion()
								)
				);
			}

			if (modrinth.version().id().equals(modGardenSubmission.platform().versionId())) {
				return new MessageResponse(
						urlProjectOrVersion == null
								? "Your Mod Garden project is up to date."
								: "Your Mod Garden project is already using the specified version."
				);
			}

			ModGarden.updateSubmission(
					modGardenSubmission.id(),
					SubmissionPlatform.modrinth(modrinth.project().id(), modrinth.version().id())
			);
			version = modrinth.version().name();
		} else if (urlProjectOrVersion != null && urlProjectOrVersion.matches(URL_REGEX)) {

			URI downloadUri;
			try {
				downloadUri = new URI(urlProjectOrVersion);
			} catch (URISyntaxException e) {
				return exceptionResponse("Failed to parse url.");
			}

			File download = null;
			try {
				download = FileUtils.download(downloadUri);
				// TODO: Validate 'minecraft' in requirements field in FMJ. I'm not doing it now because I don't want to write a FMJ parser.
				if ("fabric".equals(modGardenEvent.platform().modLoader()) && FabricModJson.isFabricMod(download)) {
					ModGarden.updateSubmission(
							modGardenSubmission.id(),
							SubmissionPlatform.downloadUrl(urlProjectOrVersion)
					);
					version = FabricModJson.getFabricModJson(download).version();
				} else {
					FileUtils.cleanupTmpFolder(download);
					return new MessageResponse(
							"Your URL project's %s version for the modloader and the game version.\nExpected %s for %s"
									.formatted(
											"version is not a valid",
											capitalizeLoaderName(modGardenEvent.platform().modLoader()),
											modGardenEvent.platform().gameVersion()
									)
					);
				}
			} catch (IllegalArgumentException | IOException | InterruptedException e) {
				if (download != null) {
					FileUtils.cleanupTmpFolder(download);
				}
				return exceptionResponse("Failed to parse downloaded file.");
			}

			FileUtils.cleanupTmpFolder(download);
		} else {
			return new MessageResponse("URL or external project/version is invalid.");
		}

		if (version != null) {
			// Do this in the case that the project metadata has changed.
			ModGardenProject finalModGardenProject = ModGarden.getProject(modGardenProject.id());
			if (finalModGardenProject == null) {
				throw new InternalServerException("Could not retrieve project after updating the submission.");
			}
			String finalFriendlyProjectName = finalModGardenProject.metadata().name();

			return new MessageResponse("Successfully updated '" + finalFriendlyProjectName + "' to version '" + version + "' for " + modGardenEvent.metadata().name() + "!");
		}

		return exceptionResponse("The target submission is invalid or doesn't exist.");
	}

	@Override
	public List<Command.Choice> getAutoCompleteChoices(String focusedOption, User user, AutoCompletionGetter autoCompletionGetter) {
		if ("project".equals(focusedOption)) {
			return getModGardenProjectChoices(user);
		}

		if ("url-or-external".equals(focusedOption)) {
			try {
				String projectId = autoCompletionGetter.getOption("project", OptionMapping::getAsString);

				if (projectId == null) {
					return Collections.emptyList();
				}

				ModGardenProject project = getModGardenProject(ModGarden.getUserByDiscordUser(user), projectId);

				if (project == null) {
					return Collections.emptyList();
				}

				ModGardenSubmission submission = getCurrentEventSubmission(project, ModGarden.getActiveEvent().event());

				if (submission == null) {
					return Collections.emptyList();
				}

				if ("modrinth".equals(submission.platform().type())) {
					return getModrinthVersionChoices(submission);
				}

				return getModrinthProjectChoices(user);
			} catch (HypertextException e) {
				GardenBot.LOG.error("", e);
			}
		}

		return Collections.emptyList();
	}
}
