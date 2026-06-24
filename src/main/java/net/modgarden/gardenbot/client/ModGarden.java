package net.modgarden.gardenbot.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.modgarden.gardenbot.GardenBot;
import net.modgarden.gardenbot.client.exception.BadRequestException;
import net.modgarden.gardenbot.client.exception.HypertextException;
import net.modgarden.gardenbot.client.exception.InternalServerException;
import net.modgarden.gardenbot.client.mod_garden.event.*;
import net.modgarden.gardenbot.client.mod_garden.project.ModGardenProject;
import net.modgarden.gardenbot.client.mod_garden.project.ModGardenSubmission;
import net.modgarden.gardenbot.client.mod_garden.project.SubmissionPlatform;
import net.modgarden.gardenbot.client.mod_garden.project.ProjectMetadata;
import net.modgarden.gardenbot.client.mod_garden.request.*;
import net.modgarden.gardenbot.client.mod_garden.role.ModGardenRole;
import net.modgarden.gardenbot.client.mod_garden.role.RoleIntegrations;
import net.modgarden.gardenbot.client.mod_garden.role.integration.DiscordRoleIntegration;
import net.modgarden.gardenbot.client.mod_garden.user.ModGardenUser;
import net.modgarden.gardenbot.client.mod_garden.user.modifiable.ModifiableUserBio;
import net.modgarden.gardenbot.client.mod_garden.user.modifiable.ModifiableUserIntegrations;
import net.modgarden.gardenbot.util.NullableWrapper;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static net.modgarden.gardenbot.GardenBot.HTTP_CLIENT;
import static net.modgarden.gardenbot.client.exception.HypertextException.hypertextException;

public class ModGarden {
	public static final String API_URL = "development".equals(System.getenv("env"))
			? "http://localhost:7070/"
			: "https://api.modgarden.net/";
	private static final String USER_AGENT = "ModGardenEvent/gardenbot/" + GardenBot.VERSION + " (modgarden.net)";

	private final static String GARDEN_BOT_CREDENTIALS = Base64.getEncoder().encodeToString(("grbot:" + GardenBot.DOTENV.get("OAUTH_SECRET")).getBytes(StandardCharsets.UTF_8));

	private ModGarden() {
	}

	@Nullable
	public static ModGardenUser getUserByDiscordUser(User discordUser) throws HypertextException {
		HttpResponse<InputStream> response;
		try {
			response = get(
					"v2/users/" + discordUser.getId() + "?by=integration_discord",
					HttpResponse.BodyHandlers.ofInputStream()
			);
		} catch (IOException | InterruptedException e) {
			throw new InternalServerException(e.getMessage());
		}

		if (response.statusCode() == 200) {
			return GardenBot.GSON.fromJson(new InputStreamReader(response.body()), ModGardenUser.class);
		}

		if (response.statusCode() == 404) {
			return null;
		}

		throw hypertextException(response);
	}

	@Nullable
	public static ModGardenUser getUserByModGardenUsername(String username) throws HypertextException {
		HttpResponse<InputStream> response;
		try {
			response = get(
					"v2/users/" + username + "?by=username",
					HttpResponse.BodyHandlers.ofInputStream()
			);
		} catch (IOException | InterruptedException e) {
			throw new InternalServerException(e.getMessage());
		}

		if (response.statusCode() == 200) {
			return GardenBot.GSON.fromJson(new InputStreamReader(response.body()), ModGardenUser.class);
		}

		if (response.statusCode() == 404) {
			return null;
		}

		throw hypertextException(response);
	}

	@Nullable
	public static ModGardenUser getUserByModGardenId(String id) throws HypertextException {
		HttpResponse<InputStream> response;
		try {
			response = get(
					"v2/users/" + id + "?by=id",
					HttpResponse.BodyHandlers.ofInputStream()
			);
		} catch (IOException | InterruptedException e) {
			throw new InternalServerException(e.getMessage());
		}

		if (response.statusCode() == 200) {
			return GardenBot.GSON.fromJson(new InputStreamReader(response.body()), ModGardenUser.class);
		}

		if (response.statusCode() == 404) {
			return null;
		}

		throw hypertextException(response);
	}

	public static ModGardenUser createUser(String username) throws HypertextException {
		String body = GardenBot.GSON.toJson(new CreateUserRequestBody(username), CreateUserRequestBody.class);

		try {
			HttpResponse<Void> response = post(
					"internal/user/create",
					HttpRequest.BodyPublishers.ofString(body),
					HttpResponse.BodyHandlers.discarding()
			);
			String location = response.headers().firstValue("Location").orElseThrow();
			return getUserByModGardenId(location.substring("/v2/users/".length()));
		} catch (IOException | InterruptedException e) {
			throw new InternalServerException(e.getMessage());
		}
	}

	public static ModGardenRole createUserRole(
			String name,
			String permissions,
			RoleIntegrations integrations
	) throws HypertextException {
		String body = GardenBot.GSON.toJson(
				new CreateUserRoleRequestBody(
						name,
						permissions,
						integrations
				),
				CreateUserRoleRequestBody.class
		);

		try {
			HttpResponse<InputStream> response = post(
					"internal/role/create",
					HttpRequest.BodyPublishers.ofString(body),
					HttpResponse.BodyHandlers.ofInputStream()
			);
			if (response.statusCode() == 201) {
				String location = response.headers().firstValue("Location").orElseThrow();
				return getUserRole(location.substring("/v2/roles/".length()));
			}
			throw hypertextException(response);
		} catch (IOException | InterruptedException e) {
			throw new InternalServerException(e.getMessage());
		}
	}

	public static ModGardenRole createUserRoleFromDiscordRole(Role discordRole) throws HypertextException {
		String body = GardenBot.GSON.toJson(
				new CreateUserRoleRequestBody(
						discordRole.getName(),
						"0",
						new RoleIntegrations(
								new DiscordRoleIntegration(
										discordRole.getId()
								)
						)
				),
				CreateUserRoleRequestBody.class
		);

		try {
			HttpResponse<InputStream> response = post(
					"internal/role/create",
					HttpRequest.BodyPublishers.ofString(body),
					HttpResponse.BodyHandlers.ofInputStream()
			);
			if (response.statusCode() == 201) {
				String location = response.headers().firstValue("Location").orElseThrow();
				return getUserRole(location.substring("/v2/roles/".length()));
			}
			throw hypertextException(response);
		} catch (IOException | InterruptedException e) {
			throw new InternalServerException(e.getMessage());
		}
	}

	public static void modifyUserRole(
			String roleId,
			ModGardenRole.Modifiable role
	) throws HypertextException {
		JsonObject root = GardenBot.GSON.toJsonTree(
				new ModifyUserRoleRequestBody(
						role.name(),
						role.permissions()
				),
				ModifyUserRoleRequestBody.class
		).getAsJsonObject();

		if (role.integrations() != null) {
			JsonObject integrations = new JsonObject();

			if (role.integrations().discord() != null) {
				if (role.integrations().discord().isPresent()) {
					integrations.add("discord", GardenBot.GSON.toJsonTree(
							role.integrations().discord().value(),
							DiscordRoleIntegration.class
					));
				} else {
					integrations.add("discord", JsonNull.INSTANCE);
				}
			}

			root.add("integrations", integrations);
		}

		String body = root.toString();

		try {
			patch(
					"internal/role/" + roleId,
					HttpRequest.BodyPublishers.ofString(body),
					HttpResponse.BodyHandlers.ofInputStream()
			);
		} catch (IOException | InterruptedException e) {
			throw new InternalServerException(e.getMessage());
		}
	}

	public static void deleteUserRole(String roleId) throws HypertextException {
		try {
			delete(
					"internal/role/" + roleId,
					HttpResponse.BodyHandlers.ofInputStream()
			);
		} catch (IOException | InterruptedException e) {
			throw new InternalServerException(e.getMessage());
		}
	}

	public static Collection<ModGardenRole> getUserRoles() throws HypertextException {
		HttpResponse<InputStream> response;
		try {
			response = get(
					"v2/roles",
					HttpResponse.BodyHandlers.ofInputStream()
			);
		} catch (IOException | InterruptedException e) {
			throw new InternalServerException(e.getMessage());
		}

		if (response.statusCode() == 200) {
			Type type = TypeToken.getParameterized(ArrayList.class, ModGardenRole.class).getType();
			return GardenBot.GSON.fromJson(JsonParser.parseReader(new InputStreamReader(response.body())), type);
		}

		throw hypertextException(response);
	}

	@Nullable
	public static ModGardenRole getUserRole(String roleId) throws HypertextException {
		if (roleId.contains(" ")) {
			throw new BadRequestException("Role ID is invalid.");
		}

		HttpResponse<InputStream> response;
		try {
			response = get(
					"v2/roles/" + roleId,
					HttpResponse.BodyHandlers.ofInputStream()
			);
		} catch (IOException | InterruptedException e) {
			throw new InternalServerException(e.getMessage());
		}

		if (response.statusCode() == 200) {
			return GardenBot.GSON.fromJson(JsonParser.parseReader(new InputStreamReader(response.body())), ModGardenRole.class);
		}

		if (response.statusCode() == 404) {
			return null;
		}

		throw hypertextException(response);
	}

	@Nullable
	public static ModGardenRole getUserRoleFromDiscordRoleId(String discordRoleId) throws HypertextException {
		HttpResponse<InputStream> response;
		try {
			response = get(
					"v2/roles/" + discordRoleId + "?by=integration_discord",
					HttpResponse.BodyHandlers.ofInputStream()
			);
		} catch (IOException | InterruptedException e) {
			throw new InternalServerException(e.getMessage());
		}

		if (response.statusCode() == 200) {
			return GardenBot.GSON.fromJson(JsonParser.parseReader(new InputStreamReader(response.body())), ModGardenRole.class);
		}

		if (response.statusCode() == 404) {
			return null;
		}

		throw hypertextException(response);
	}

	public static void addDiscordRolesToModGardenUser(Member member, List<Role> roles) throws HypertextException {
		ModGardenUser user = ModGarden.getUserByDiscordUser(member.getUser());
		if (user == null)
			return;

		for (Role role : roles) {
			ModGardenRole modGardenRole = ModGarden.getUserRoleFromDiscordRoleId(role.getId());

			if (modGardenRole == null)
				continue;


			ModGarden.addUserRole(user, modGardenRole);
		}
	}

	public static void removeDiscordRolesFromModGardenUser(Member member, List<Role> roles) throws HypertextException {
		for (Role role : roles) {
			ModGardenRole modGardenRole = ModGarden.getUserRoleFromDiscordRoleId(role.getId());
			if (modGardenRole == null)
				continue;
			ModGarden.removeUserRole(ModGarden.getUserByDiscordUser(member.getUser()), modGardenRole);
		}
	}

	public static ModGardenGenre getGenre(String slug) throws HypertextException {
		HttpResponse<InputStream> response;
		try {
			response = get(
					"v2/genres/" + slug,
					HttpResponse.BodyHandlers.ofInputStream()
			);
		} catch (IOException | InterruptedException e) {
			throw new InternalServerException(e.getMessage());
		}

		if (response.statusCode() == 200) {
			return GardenBot.GSON.fromJson(new InputStreamReader(response.body()), ModGardenGenre.class);
		}

		if (response.statusCode() == 404) {
			return null;
		}

		throw hypertextException(response);
	}


	@Nullable
	public static ModGardenProject createProject(String name) throws HypertextException {
		String body = GardenBot.GSON.toJson(new CreateProjectRequestBody(new ProjectMetadata(null, name)), CreateProjectRequestBody.class);

		HttpResponse<InputStream> response;

		try {
			response = post(
					"v2/projects",
					HttpRequest.BodyPublishers.ofString(body),
					HttpResponse.BodyHandlers.ofInputStream()
			);
			if (response.statusCode() == 201) {
				String location = response.headers().firstValue("Location").orElseThrow();
				return getProject(location.substring("/v2/projects/".length()));
			}
			throw hypertextException(response);
		} catch (IOException | InterruptedException e) {
			throw new InternalServerException(e.getMessage());
		}
	}

	public static void deleteProject(String projectId) throws HypertextException {
		HttpResponse<InputStream> response;
		try {
			response = delete(
					"v2/projects/" + projectId,
					HttpResponse.BodyHandlers.ofInputStream()
			);
		} catch (IOException | InterruptedException e) {
			throw new InternalServerException(e.getMessage());
		}

		if (response.statusCode() == 200) {
			return;
		}

		throw hypertextException(response);
	}

	@Nullable
	public static ModGardenProject getProject(String projectId) throws HypertextException {
		HttpResponse<InputStream> response;
		try {
			response = get(
					"v2/projects/" + projectId,
					HttpResponse.BodyHandlers.ofInputStream()
			);
		} catch (IOException | InterruptedException e) {
			throw new InternalServerException(e.getMessage());
		}

		if (response.statusCode() == 200) {
			return GardenBot.GSON.fromJson(JsonParser.parseReader(new InputStreamReader(response.body())), ModGardenProject.class);
		}

		if (response.statusCode() == 404) {
			return null;
		}

		throw hypertextException(response);
	}

	@Nullable
	public static ModGardenProject getProjectFromModId(String modId) throws HypertextException {
		HttpResponse<InputStream> response;
		try {
			response = get(
					"v2/projects/" + modId + "?by=mod_id",
					HttpResponse.BodyHandlers.ofInputStream()
			);
		} catch (IOException | InterruptedException e) {
			throw new InternalServerException(e.getMessage());
		}

		if (response.statusCode() == 200) {
			return GardenBot.GSON.fromJson(JsonParser.parseReader(new InputStreamReader(response.body())), ModGardenProject.class);
		}

		if (response.statusCode() == 404) {
			return null;
		}

		throw hypertextException(response);
	}

	public static ModGardenSubmission createSubmission(String modGardenProjectId,
	                                                           String modGardenEventId,
	                                                           SubmissionPlatform platform) throws HypertextException {
		String body = GardenBot.GSON.toJson(
				new CreateSubmissionModrinthRequestBody(
						modGardenProjectId,
						modGardenEventId,
						platform
				),
				CreateSubmissionModrinthRequestBody.class
		);

		try {
			HttpResponse<InputStream> response = post(
					"v2/submissions",
					HttpRequest.BodyPublishers.ofString(body),
					HttpResponse.BodyHandlers.ofInputStream()
			);
			if (response.statusCode() == 201) {
				String location = response.headers().firstValue("Location").orElseThrow();
				return getSubmission(location.substring("/v2/submissions/".length()));
			}
			throw hypertextException(response);
		} catch (IOException | InterruptedException e) {
			throw new InternalServerException(e.getMessage());
		}
	}

	public static void updateSubmission(String submissionId,
	                                    SubmissionPlatform platform) throws HypertextException {
		String body = GardenBot.GSON.toJson(
				new ModifySubmissionRequestBody(
						platform
				),
				ModifySubmissionRequestBody.class
		);

		try {
			HttpResponse<InputStream> response = patch(
					"v2/submissions/" + submissionId,
					HttpRequest.BodyPublishers.ofString(body),
					HttpResponse.BodyHandlers.ofInputStream()
			);
			if (response.statusCode() == 200) {
				return;
			}
			throw hypertextException(response);
		} catch (IOException | InterruptedException e) {
			throw new InternalServerException(e.getMessage());
		}
	}

	public static void deleteSubmission(String submissionId) throws HypertextException {
		HttpResponse<InputStream> response;
		try {
			response = delete(
					"v2/submissions/" + submissionId,
					HttpResponse.BodyHandlers.ofInputStream()
			);
		} catch (IOException | InterruptedException e) {
			throw new InternalServerException(e.getMessage());
		}

		if (response.statusCode() == 200) {
			return;
		}

		throw hypertextException(response);
	}

	@Nullable
	public static ModGardenSubmission getSubmission(String submissionId) throws HypertextException {
		HttpResponse<InputStream> response;
		try {
			response = get(
					"v2/submissions/%s".formatted(submissionId),
					HttpResponse.BodyHandlers.ofInputStream()
			);
		} catch (IOException | InterruptedException e) {
			throw new InternalServerException(e.getMessage());
		}

		if (response.statusCode() == 200) {
			return GardenBot.GSON.fromJson(JsonParser.parseReader(new InputStreamReader(response.body())), ModGardenSubmission.class);
		}

		if (response.statusCode() == 404) {
			return null;
		}

		throw hypertextException(response);
	}

	public static List<ModGardenSubmission> getSubmissions(String genreSlug, String eventSlug) throws HypertextException {
		HttpResponse<InputStream> response;
		try {
			response = get(
					"v2/events/%s/%s/submissions"
							.formatted(genreSlug, eventSlug),
					HttpResponse.BodyHandlers.ofInputStream()
			);
		} catch (IOException | InterruptedException e) {
			throw new InternalServerException(e.getMessage());
		}

		if (response.statusCode() != 200) {
			return Collections.emptyList();
		}

		List<ModGardenSubmission> submissions = new ArrayList<>();
		JsonElement submissionsJson = JsonParser.parseReader(new InputStreamReader(response.body()));

		for (JsonElement element : submissionsJson.getAsJsonArray()) {
			ModGardenSubmission submission = GardenBot.GSON.fromJson(element, ModGardenSubmission.class);
			submissions.add(submission);
		}

		return submissions;
	}

	public static ModGardenEvent createEvent(
			String genreId,
			String slug,
			EventMetadata metadata,
			EventTimes times,
			EventPlatform platform
	) throws HypertextException {
		String body = GardenBot.GSON.toJson(
				new CreateEventRequestBody(
						genreId,
						slug,
						metadata,
						times,
						platform
				),
				CreateEventRequestBody.class
		);

		try {
			HttpResponse<InputStream> response = post(
					"internal/event/create",
					HttpRequest.BodyPublishers.ofString(body),
					HttpResponse.BodyHandlers.ofInputStream()
			);
			if (response.statusCode() == 201) {
				String location = response.headers().firstValue("Location").orElseThrow();
				return getEventByIds(genreId, location.substring("/v2/events/%s/".formatted(genreId).length()));
			}
			throw hypertextException(response);
		} catch (IOException | InterruptedException e) {
			throw new InternalServerException(e.getMessage());
		}
	}

	public static void modifyEvent(
			String genreSlug,
			String eventSlug,
			@Nullable EventMetadata.Modifiable metadata,
			@Nullable EventTimes.Modifiable times,
			@Nullable EventPlatform.Modifiable platform,
			Map<String, NullableWrapper<String>> roles
	) throws HypertextException {
		JsonObject root = GardenBot.GSON.toJsonTree(
				new ModifyEventRequestBody(
						metadata,
						times,
						platform,
						roles
				),
				ModifyEventRequestBody.class
		).getAsJsonObject();

		for (Map.Entry<String, NullableWrapper<String>> entry : roles.entrySet()) {
			if (entry.getValue().isEmpty()) {
				root.getAsJsonObject("roles").add(entry.getKey(), JsonNull.INSTANCE);
			}
		}

		if (metadata != null && metadata.description() != null && metadata.description().isEmpty()) {
			root.getAsJsonObject("metadata").add("description", JsonNull.INSTANCE);
		}

		String body = root.toString();

		try {
			HttpResponse<InputStream> response = patch(
					"internal/event/" + genreSlug + "/" + eventSlug,
					HttpRequest.BodyPublishers.ofString(body),
					HttpResponse.BodyHandlers.ofInputStream()
			);

			if (response.statusCode() != 200) {
				throw hypertextException(response);
			}
		} catch (IOException | InterruptedException e) {
			throw new InternalServerException(e.getMessage());
		}
	}

	public static void addRolesToEvent(String genreId, String eventId, Map<String, String> roleMap) throws HypertextException {
		String body = GardenBot.GSON.toJson(
				new ModifyAddEventRolesRequestBody(
						roleMap
				),
				ModifyAddEventRolesRequestBody.class
		);

		HttpResponse<InputStream> response;
		try {
			 response = post(
					"internal/event/modify/%s/%s".formatted(genreId, eventId),
					HttpRequest.BodyPublishers.ofString(body),
					HttpResponse.BodyHandlers.ofInputStream()
			 );
		} catch (IOException | InterruptedException e) {
			throw new InternalServerException(e.getMessage());
		}

		if (response.statusCode() == 200) {
			return;
		}
		throw hypertextException(response);
	}

	@Nullable
	public static ModGardenEvent getEventByIds(String genreId, String eventId) throws HypertextException {
		HttpResponse<InputStream> response;
		try {
			response = get(
					"v2/events/%s/%s?by=id".formatted(genreId, eventId),
					HttpResponse.BodyHandlers.ofInputStream()
			);
		} catch (IOException | InterruptedException e) {
			throw new InternalServerException(e.getMessage());
		}

		if (response.statusCode() == 200) {
			return GardenBot.GSON.fromJson(JsonParser.parseReader(new InputStreamReader(response.body())), ModGardenEvent.class);
		}

		if (response.statusCode() == 404) {
			return null;
		}

		throw hypertextException(response);
	}

	@Nullable
	public static ModGardenEvent getEventBySlugs(String genreSlug, String eventSlug) throws HypertextException {
		HttpResponse<InputStream> response;
		try {
			response = get(
					"v2/events/%s/%s".formatted(genreSlug, eventSlug),
					HttpResponse.BodyHandlers.ofInputStream()
			);
		} catch (IOException | InterruptedException e) {
			throw new InternalServerException(e.getMessage());
		}

		if (response.statusCode() == 200) {
			return GardenBot.GSON.fromJson(JsonParser.parseReader(new InputStreamReader(response.body())), ModGardenEvent.class);
		}

		if (response.statusCode() == 404) {
			return null;
		}

		throw hypertextException(response);
	}

	@Nullable
	public static String getGenreIdBySlug(String genreSlug) throws HypertextException {
		HttpResponse<InputStream> response;
		try {
			response = get(
					"v2/genres/%s?by=slug&with=id".formatted(genreSlug),
					HttpResponse.BodyHandlers.ofInputStream()
			);
		} catch (IOException | InterruptedException e) {
			throw new InternalServerException(e.getMessage());
		}

		if (response.statusCode() == 200) {
			return GardenBot.GSON.fromJson(JsonParser.parseReader(new InputStreamReader(response.body())), String.class);
		}

		if (response.statusCode() == 404) {
			return null;
		}

		throw hypertextException(response);
	}

	@Nullable
	public static String getEventIdBySlugs(String genreSlug, String eventSlug) throws HypertextException {
		HttpResponse<InputStream> response;
		try {
			response = get(
					"v2/events/%s/%s?by=slug&with=id".formatted(genreSlug, eventSlug),
					HttpResponse.BodyHandlers.ofInputStream()
			);
		} catch (IOException | InterruptedException e) {
			throw new InternalServerException(e.getMessage());
		}

		if (response.statusCode() == 200) {
			return GardenBot.GSON.fromJson(JsonParser.parseReader(new InputStreamReader(response.body())), String.class);
		}

		if (response.statusCode() == 404) {
			return null;
		}

		throw hypertextException(response);
	}

	public static GenreAndEvent getDevelopmentTimeEvent() throws HypertextException {
		return getEventWithinTimeframe(
				EventTimes::registrationOpen,
				EventTimes::packFreeze
		);
	}

	public static GenreAndEvent getActiveEvent() throws HypertextException {
		return getEventWithinTimeframe(
				EventTimes::registrationOpen,
				EventTimes::packFreeze
		);
	}

	public static void transferProjectOwnership(ModGardenProject project, ModGardenUser user) throws HypertextException {
		JsonObject bodyJson = new JsonObject();

		try {
			JsonObject teamJson = new JsonObject();
			JsonObject permissionsJson = new JsonObject();

			teamJson.addProperty(user.id(), "Member");
			bodyJson.add("team", teamJson);
			permissionsJson.addProperty(user.id(), "1");
			bodyJson.add("permissions", permissionsJson);

			HttpResponse<InputStream> addUserResponse = patch(
					"v2/projects/" + project.id(),
					HttpRequest.BodyPublishers.ofString(bodyJson.toString()),
					HttpResponse.BodyHandlers.ofInputStream()
			);
			if (addUserResponse.statusCode() != 200) {
				throw hypertextException(addUserResponse);
			}

			teamJson.remove(user.id());
			teamJson.add("grbot", JsonNull.INSTANCE);
			bodyJson.add("team", teamJson);

			HttpResponse<InputStream> removeGardenBotResponse = patch(
					"v2/projects/" + project.id(),
					HttpRequest.BodyPublishers.ofString(bodyJson.toString()),
					HttpResponse.BodyHandlers.ofInputStream()
			);
			if (addUserResponse.statusCode() != 200) {
				throw hypertextException(removeGardenBotResponse);
			}
		} catch (IOException | InterruptedException e) {
			throw new InternalServerException(e.getMessage());
		}
	}

	public static void addTeamMembers(ModGardenProject project, Map<ModGardenUser, String> usersToRole) throws HypertextException {
		JsonElement requestJson = GardenBot.GSON.toJsonTree(
				usersToRole.entrySet()
						.stream()
						.map(entry -> Map.entry(entry.getKey().id(), entry.getValue()))
						.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)),
				Map.class
		);
		try {
			patch(
					"v2/projects/" + project.id() + "/members",
					HttpRequest.BodyPublishers.ofString(requestJson.toString()),
					HttpResponse.BodyHandlers.discarding()
			);
		} catch (IOException | InterruptedException e) {
			throw new InternalServerException(e.getMessage());
		}
	}

	public static void modifyUser(ModGardenUser user, ModifyUserRequestBody request) throws HypertextException {
		String body = GardenBot.GSON.toJson(request, ModifyUserRequestBody.class);

		HttpResponse<InputStream> response;
		try {
			response = patch(
					"internal/user/modify/" + user.id(),
					HttpRequest.BodyPublishers.ofString(body),
					HttpResponse.BodyHandlers.ofInputStream()
			);
		} catch (IOException | InterruptedException e) {
			throw new InternalServerException(e.getMessage());
		}

		if (response.statusCode() != 200) {
			throw hypertextException(response);
		}
	}

	/// Modifies the username of the specified user.
	/// @param user The user to modify.
	/// @param username The value to change the user's username to.
	public static void modifyUsername(ModGardenUser user, String username) throws HypertextException {
		modifyUser(user, new ModifyUserRequestBody(username, null, null, Collections.emptyList()));
	}

	/// Modifies the user bio of the specified user.
	/// @param user The user to modify.
	/// @param bio	The bio to use for the user.
	/// @see ModifiableUserBio
	public static void modifyUserBio(ModGardenUser user, ModifiableUserBio bio) throws HypertextException {
		modifyUser(user, new ModifyUserRequestBody(
				null,
				bio,
				null,
				Collections.emptyList())
		);
	}

	/// Modifies the user integrations of the specified users.
	/// @param user The user to modify.
	/// @param integrations A modifiable integrations object.
	/// @see ModifiableUserIntegrations
	public static void modifyUserIntegrations(ModGardenUser user, @Nullable ModifiableUserIntegrations integrations) throws HypertextException {
		modifyUser(user, new ModifyUserRequestBody(
				null,
				null,
				integrations,
				Collections.emptyList()
		));
	}

	public static void addUserRole(ModGardenUser user, ModGardenRole role) throws HypertextException {
		modifyUser(user, new ModifyUserRequestBody(
				null,
				null,
				null,
				List.of(role.id()))
		);
	}

	public static void removeUserRole(ModGardenUser user, ModGardenRole role) throws HypertextException {
		modifyUser(user, new ModifyUserRequestBody(null, null, null, List.of("!" + role.id())));
	}

	@Nullable
	private static GenreAndEvent getEventWithinTimeframe(Function<EventTimes, String> minTimeFrame, Function<EventTimes, String> maxTimeFrame) throws HypertextException {
		HttpResponse<InputStream> genresResponse;
		try {
			genresResponse = get(
					"v2/genres",
					HttpResponse.BodyHandlers.ofInputStream()
			);
		} catch (IOException | InterruptedException e) {
			throw new InternalServerException(e.getMessage());
		}

		JsonElement genres = JsonParser.parseReader(new InputStreamReader(genresResponse.body()));

		if (!genres.isJsonArray()) {
			throw new InternalServerException("Mod Garden genres endpoint is not a list.");
		}

		for (JsonElement element : genres.getAsJsonArray()) {
			ModGardenGenre genre = GardenBot.GSON.fromJson(element, ModGardenGenre.class);

			for (String eventId : genre.events()) {
				HttpResponse<InputStream> eventResponse;
				try {
					eventResponse = get(
							"v2/events/" + genre.id() + "/" + eventId + "?by=id",
							HttpResponse.BodyHandlers.ofInputStream()
					);
				} catch (IOException | InterruptedException e) {
					throw new InternalServerException(e.getMessage());
				}

				ModGardenEvent event = GardenBot.GSON.fromJson(
						JsonParser.parseReader(new InputStreamReader(eventResponse.body())),
						ModGardenEvent.class
				);

				long now = Instant.now().toEpochMilli();

				long registrationOpen = Long.parseLong(minTimeFrame.apply(event.times()));
				long packFreeze = Long.parseLong(maxTimeFrame.apply(event.times()));

				if (now >= registrationOpen && now < packFreeze) {
					return new GenreAndEvent(genre, event);
				}
			}
		}

		return null;
	}

	private static <T> HttpResponse<T> get(String endpoint, HttpResponse.BodyHandler<T> bodyHandler, String... headers) throws IOException, InterruptedException {
		var req = HttpRequest.newBuilder(URI.create(API_URL + endpoint))
				.header("User-Agent", USER_AGENT)
				.header("Authorization", "Basic " + GARDEN_BOT_CREDENTIALS);
		if (headers.length > 0) {
			req.headers(headers);
		}

		return HTTP_CLIENT.send(req.build(), bodyHandler);
	}

	private static <T> HttpResponse<T> post(String endpoint, HttpRequest.BodyPublisher bodyPublisher, HttpResponse.BodyHandler<T> bodyHandler, String... headers) throws IOException, InterruptedException {
		var req = HttpRequest.newBuilder(URI.create(API_URL + endpoint))
				.header("User-Agent", USER_AGENT)
				.header("Authorization", "Basic " + GARDEN_BOT_CREDENTIALS);
		if (headers.length > 0) {
			req.headers(headers);
		}
		req.POST(bodyPublisher);

		return HTTP_CLIENT.send(req.build(), bodyHandler);
	}

	private static <T> HttpResponse<T> patch(String endpoint, HttpRequest.BodyPublisher bodyPublisher, HttpResponse.BodyHandler<T> bodyHandler, String... headers) throws IOException, InterruptedException {
		var req = HttpRequest.newBuilder(URI.create(API_URL + endpoint))
				.header("User-Agent", USER_AGENT)
				.header("Authorization", "Basic " + GARDEN_BOT_CREDENTIALS);
		if (headers.length > 0) {
			req.headers(headers);
		}
		req.method("PATCH", bodyPublisher);

		return HTTP_CLIENT.send(req.build(), bodyHandler);
	}

	private static <T> HttpResponse<T> delete(String endpoint, HttpResponse.BodyHandler<T> bodyHandler, String... headers) throws IOException, InterruptedException {
		var req = HttpRequest.newBuilder(URI.create(API_URL + endpoint))
				.header("User-Agent", USER_AGENT)
				.header("Authorization", "Basic " + GARDEN_BOT_CREDENTIALS);
		if (headers.length > 0) {
			req.headers(headers);
		}
		req.method("DELETE", HttpRequest.BodyPublishers.noBody());

		return HTTP_CLIENT.send(req.build(), bodyHandler);
	}

	public record ExceptionPage(String description) {
	}
}
