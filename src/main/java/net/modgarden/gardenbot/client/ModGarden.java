package net.modgarden.gardenbot.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.dv8tion.jda.api.entities.User;
import net.modgarden.gardenbot.GardenBot;
import net.modgarden.gardenbot.client.exception.HypertextException;
import net.modgarden.gardenbot.client.modgarden.event.EventTimes;
import net.modgarden.gardenbot.client.modgarden.event.GenreAndEvent;
import net.modgarden.gardenbot.client.modgarden.event.ModGardenEvent;
import net.modgarden.gardenbot.client.modgarden.event.ModGardenGenre;
import net.modgarden.gardenbot.client.modgarden.project.ModGardenProject;
import net.modgarden.gardenbot.client.modgarden.project.ModGardenSubmission;
import net.modgarden.gardenbot.client.modgarden.request.CreateUserRequestBody;
import net.modgarden.gardenbot.client.modgarden.request.ModifyUserRequestBody;
import net.modgarden.gardenbot.client.modgarden.role.ModGardenRole;
import net.modgarden.gardenbot.client.modgarden.user.ModGardenUser;
import net.modgarden.gardenbot.client.modgarden.user.UserBio;
import net.modgarden.gardenbot.client.modgarden.user.UserIntegrations;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ModGarden {
	public static final String API_URL = "development".equals(System.getenv("env"))
			? "http://localhost:7070/"
			: "https://api.modgarden.net/";

	private static final String USER_AGENT = "ModGardenEvent/gardenbot/" + GardenBot.VERSION + " (modgarden.net)";
	private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

	private final static String GARDEN_BOT_CREDENTIALS = Base64.getEncoder().encodeToString(("grbot:" + GardenBot.DOTENV.get("OAUTH_SECRET")).getBytes(StandardCharsets.UTF_8));

	private ModGarden() {
	}

	@Nullable
	public static ModGardenUser getUserByDiscordId(User discordUser) throws HypertextException {
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

	public static ModGardenUser createUser(String username) throws HypertextException {
		String body = GardenBot.GSON.toJson(new CreateUserRequestBody(username), CreateUserRequestBody.class);

		try {
			HttpResponse<Void> response = post(
					"/internal/user/create",
					HttpRequest.BodyPublishers.ofString(body),
					HttpResponse.BodyHandlers.discarding()
			);
			String location = response.headers().firstValue("Location").orElseThrow();
			return getUserByModGardenId(location.substring("/v2/users/".length()));
		} catch (IOException | InterruptedException | NoSuchElementException e) {
			throw new HypertextException(500, e.getMessage());
		}
	}

	@Nullable
	public static ModGardenRole getParticipantRole(ModGardenEvent event) throws HypertextException {
		String participantRoleId = event.roles().participant();

		HttpResponse<InputStream> response;
		try {
			response = get(
					"v2/roles/" + participantRoleId,
					HttpResponse.BodyHandlers.ofInputStream()
			);
		} catch (IOException | InterruptedException e) {
			throw new HypertextException(500, e.getMessage());
		}

		if (response.statusCode() != 200) {
			return GardenBot.GSON.fromJson(JsonParser.parseReader(new InputStreamReader(response.body())), ModGardenRole.class);
		}

		if (response.statusCode() == 404) {
			return null;
		}

		throw hypertextException(response);
	}

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

		return null;
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

	public static GenreAndEvent getActiveEvent() throws HypertextException {
		return getEventWithinTimeframe(
				EventTimes::registrationOpen,
				EventTimes::packFreeze
		);
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
	private static ModGardenUser getUserByModGardenId(String id) throws HypertextException {
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
		return new HypertextException(response.statusCode(), GardenBot.GSON.fromJson(new InputStreamReader(response.body()), ExceptionPage.class).description);
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


	private record ExceptionPage(String description) {
	}
}
