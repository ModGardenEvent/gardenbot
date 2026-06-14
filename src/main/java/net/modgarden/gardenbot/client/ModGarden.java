package net.modgarden.gardenbot.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.dv8tion.jda.api.entities.User;
import net.modgarden.gardenbot.GardenBot;
import net.modgarden.gardenbot.client.exception.HypertextException;
import net.modgarden.gardenbot.client.mod_garden.event.EventTimes;
import net.modgarden.gardenbot.client.mod_garden.event.GenreAndEvent;
import net.modgarden.gardenbot.client.mod_garden.event.ModGardenEvent;
import net.modgarden.gardenbot.client.mod_garden.event.ModGardenGenre;
import net.modgarden.gardenbot.client.mod_garden.project.ModGardenProject;
import net.modgarden.gardenbot.client.mod_garden.project.ModGardenSubmission;
import net.modgarden.gardenbot.client.mod_garden.project.ModrinthSubmissionPlatform;
import net.modgarden.gardenbot.client.mod_garden.project.ProjectMetadata;
import net.modgarden.gardenbot.client.mod_garden.request.CreateModrinthSubmissionRequestBody;
import net.modgarden.gardenbot.client.mod_garden.request.CreateProjectRequestBody;
import net.modgarden.gardenbot.client.mod_garden.request.CreateUserRequestBody;
import net.modgarden.gardenbot.client.mod_garden.request.ModifyUserRequestBody;
import net.modgarden.gardenbot.client.mod_garden.role.ModGardenRole;
import net.modgarden.gardenbot.client.mod_garden.user.ModGardenUser;
import net.modgarden.gardenbot.client.mod_garden.user.UserBio;
import net.modgarden.gardenbot.client.mod_garden.user.UserIntegrations;
import net.modgarden.gardenbot.client.modrinth.ModrinthProject;
import net.modgarden.gardenbot.client.modrinth.ModrinthVersion;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static net.modgarden.gardenbot.GardenBot.HTTP_CLIENT;

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
			throw new HypertextException(500, e.getMessage());
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
			throw new HypertextException(500, e.getMessage());
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
			throw new HypertextException(500, e.getMessage());
		}
	}

	@Nullable
	public static ModGardenRole getRole(String roleId) throws HypertextException {
		HttpResponse<InputStream> response;
		try {
			response = get(
					"v2/roles/" + roleId,
					HttpResponse.BodyHandlers.ofInputStream()
			);
		} catch (IOException | InterruptedException e) {
			throw new HypertextException(500, e.getMessage());
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
	public static ModGardenRole getRoleFromDiscordRoleId(String discordRoleId) throws HypertextException {
		HttpResponse<InputStream> response;
		try {
			response = get(
					"v2/roles/" + discordRoleId + "?by=integration_discord",
					HttpResponse.BodyHandlers.ofInputStream()
			);
		} catch (IOException | InterruptedException e) {
			throw new HypertextException(500, e.getMessage());
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
			throw new HypertextException(500, e.getMessage());
		}
	}

	public static void deleteProject(ModGardenProject project) throws HypertextException {
		HttpResponse<InputStream> response;
		try {
			response = delete(
					"v2/projects/" + project.id(),
					HttpResponse.BodyHandlers.ofInputStream()
			);
		} catch (IOException | InterruptedException e) {
			throw new HypertextException(500, e.getMessage());
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
			throw new HypertextException(500, e.getMessage());
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
			throw new HypertextException(500, e.getMessage());
		}

		if (response.statusCode() == 200) {
			return GardenBot.GSON.fromJson(JsonParser.parseReader(new InputStreamReader(response.body())), ModGardenProject.class);
		}

		if (response.statusCode() == 404) {
			return null;
		}

		throw hypertextException(response);
	}

	public static ModGardenSubmission createModrinthSubmission(ModGardenProject modGardenProject,
															   ModGardenEvent event,
															   ModrinthProject modrinthProject,
															   ModrinthVersion modrinthVersion) throws HypertextException {
		String body = GardenBot.GSON.toJson(
				new CreateModrinthSubmissionRequestBody(
						modGardenProject.id(),
						event.id(),
						new ModrinthSubmissionPlatform(modrinthProject.id(), modrinthVersion.id())
				),
				CreateModrinthSubmissionRequestBody.class
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
			throw new HypertextException(500, e.getMessage());
		}
	}

	public static void deleteSubmission(ModGardenSubmission submission) throws HypertextException {
		HttpResponse<InputStream> response;
		try {
			response = delete(
					"v2/submissions/" + submission.id(),
					HttpResponse.BodyHandlers.ofInputStream()
			);
		} catch (IOException | InterruptedException e) {
			throw new HypertextException(500, e.getMessage());
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
			throw new HypertextException(500, e.getMessage());
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
			throw new HypertextException(500, e.getMessage());
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

	public static GenreAndEvent getRegistrableEvent() throws HypertextException {
		return getEventWithinTimeframe(
				EventTimes::registrationOpen,
				EventTimes::registrationClose
		);
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
			throw new HypertextException(500, e.getMessage());
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
			throw new HypertextException(500, e.getMessage());
		}
	}

	public static void modifyUsername(ModGardenUser user, @Nullable String username) throws HypertextException {
		modifyUser(user, new ModifyUserRequestBody(username, null, null, Collections.emptyList()));
	}

	// TODO: Allow removals of values.
	public static void modifyUserBio(ModGardenUser user, @Nullable UserBio bio) throws HypertextException {
		modifyUser(user, new ModifyUserRequestBody(null, bio, null, Collections.emptyList()));
	}

	// TODO: Allow removals of values.
	public static void modifyUserIntegrations(ModGardenUser user, @Nullable UserIntegrations integrations) throws HypertextException {
		modifyUser(user, new ModifyUserRequestBody(null, null, integrations, Collections.emptyList()));
	}

	public static void addUserRole(ModGardenUser user, ModGardenRole role) throws HypertextException {
		modifyUser(user, new ModifyUserRequestBody(null, null, null, List.of(role.id())));
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
			throw new HypertextException(500, e.getMessage());
		}

		JsonElement genres = JsonParser.parseReader(new InputStreamReader(genresResponse.body()));

		if (!genres.isJsonArray()) {
			throw new HypertextException(500, "Mod Garden genres endpoint is not a list.");
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
					throw new HypertextException(500, e.getMessage());
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


	private static void modifyUser(ModGardenUser user, ModifyUserRequestBody request) throws HypertextException {
		String body = GardenBot.GSON.toJson(request, ModifyUserRequestBody.class);

		HttpResponse<InputStream> response;
		try {
			response = patch(
					"internal/user/modify/" + user.id(),
					HttpRequest.BodyPublishers.ofString(body),
					HttpResponse.BodyHandlers.ofInputStream()
			);
		} catch (IOException | InterruptedException e) {
			throw new HypertextException(500, e.getMessage());
		}

		if (response.statusCode() != 200) {
			throw hypertextException(response);
		}
	}

	private static HypertextException hypertextException(HttpResponse<InputStream> response) {
		return new HypertextException(response.statusCode(), GardenBot.GSON.fromJson(new InputStreamReader(response.body()), ExceptionPage.class).description());
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

	private record ExceptionPage(String description) {
	}
}
