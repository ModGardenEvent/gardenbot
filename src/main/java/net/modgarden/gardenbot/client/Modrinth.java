package net.modgarden.gardenbot.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.modgarden.gardenbot.GardenBot;
import net.modgarden.gardenbot.client.exception.HypertextException;
import net.modgarden.gardenbot.client.mod_garden.event.ModGardenEvent;
import net.modgarden.gardenbot.client.modrinth.ModrinthFile;
import net.modgarden.gardenbot.client.modrinth.ModrinthProject;
import net.modgarden.gardenbot.client.modrinth.ModrinthVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import static net.modgarden.gardenbot.GardenBot.HTTP_CLIENT;

public class Modrinth {
	public static final String API_URL = "https://api.modrinth.com/";

	private static final String USER_AGENT = "ModGardenEvent/gardenbot/" + GardenBot.VERSION + " (modgarden.net)";

	public static ModrinthProject getProject(String projectIdOrSlug) throws HypertextException {
		HttpResponse<InputStream> response;
		try {
			response = get(
					"v2/project/%s".formatted(projectIdOrSlug),
					HttpResponse.BodyHandlers.ofInputStream()
			);
		} catch (IOException | InterruptedException e) {
			throw new HypertextException(500, e.getMessage());
		}

		if (response.statusCode() == 200) {
			return GardenBot.GSON.fromJson(JsonParser.parseReader(new InputStreamReader(response.body())), ModrinthProject.class);
		}

		return null;
	}

	public static List<ModrinthProject> getProjectsFromUser(String userId) throws HypertextException {
		HttpResponse<InputStream> response;
		try {
			response = get(
					"v2/user/%s/projects".formatted(userId),
					HttpResponse.BodyHandlers.ofInputStream()
			);
		} catch (IOException | InterruptedException e) {
			throw new HypertextException(500, e.getMessage());
		}

		if (response.statusCode() != 200) {
			return Collections.emptyList();
		}

		List<ModrinthProject> projects = new ArrayList<>();
		JsonElement submissionsJson = JsonParser.parseReader(new InputStreamReader(response.body()));

		for (JsonElement element : submissionsJson.getAsJsonArray()) {
			ModrinthProject project = GardenBot.GSON.fromJson(element, ModrinthProject.class);
			projects.add(project);
		}

		return projects;
	}

	@Nullable
	public static ModrinthVersion getLatestVersionOfProject(String projectId, ModGardenEvent event) throws HypertextException {
		return Modrinth.getVersionsFromProject(projectId)
				.stream()
				.filter(mrVersion -> Modrinth.forMinecraftLoaderAndVersionOfEvent(mrVersion, event))
				.max(Comparator.comparingLong(modrinthVersion ->
						ZonedDateTime.parse(
								modrinthVersion.datePublished(),
								DateTimeFormatter.ISO_OFFSET_DATE_TIME
						).toInstant().toEpochMilli()
				)).orElse(null);
	}

	public static List<ModrinthVersion> getVersionsFromProject(String projectId) throws HypertextException {
		HttpResponse<InputStream> response;
		try {
			response = get(
					"v2/project/%s/version".formatted(projectId),
					HttpResponse.BodyHandlers.ofInputStream()
			);
		} catch (IOException | InterruptedException e) {
			throw new HypertextException(500, e.getMessage());
		}

		List<ModrinthVersion> versions = new ArrayList<>();
		JsonElement submissionsJson = JsonParser.parseReader(new InputStreamReader(response.body()));

		for (JsonElement element : submissionsJson.getAsJsonArray()) {
			ModrinthVersion version = GardenBot.GSON.fromJson(element, ModrinthVersion.class);
			versions.add(version);
		}

		return versions;
	}

	public static boolean forMinecraftLoaderAndVersionOfEvent(ModrinthVersion modrinthVersion,
	                                                          ModGardenEvent modGardenEvent) {
		// Note: We do not intend on running any NeoForge based events.
		// Sinytra Connector does not need to be accounted for...
		// TODO: Support resource packs, data packs, and other bits of content later...
		if (!modrinthVersion.loaders().contains(modGardenEvent.platform().modLoader())) {
			return false;
		}

		// TODO: Maybe better parsing as soon as we confirm that Mojang will NOT make breaking changes in patch versions.
		return modrinthVersion.gameVersions().contains(modGardenEvent.platform().gameVersion());
	}

	public static ModrinthVersion getVersion(String modrinthVersionId) throws HypertextException {
		HttpResponse<InputStream> response;
		try {
			response = get(
					"v2/version/%s".formatted(modrinthVersionId),
					HttpResponse.BodyHandlers.ofInputStream()
			);
		} catch (IOException | InterruptedException e) {
			throw new HypertextException(500, e.getMessage());
		}

		if (response.statusCode() == 200) {
			return GardenBot.GSON.fromJson(JsonParser.parseReader(new InputStreamReader(response.body())), ModrinthVersion.class);
		}

		return null;
	}

	public static String getModIdFromVersion(ModrinthVersion version) throws HypertextException {
		if (version.loaders().contains("fabric")) {
			for (ModrinthFile file : version.files()) {
				if (file.primary()) {
					return getModVersionFromFabricModJson(URI.create(file.url()));
				}
			}
		}
		throw new HypertextException(500, "None of the Modrinth version's loaders are supported by GardenBot.");
	}

	public static String getModVersionFromFabricModJson(@NotNull URI jarUri) throws HypertextException {
		var request = HttpRequest.newBuilder()
				.header("User-Agent", USER_AGENT)
				.uri(jarUri)
				.build();

		Path temporaryFolder = Path.of("./.tmp");
		try {
			HttpResponse<Path> response = HTTP_CLIENT.send(
					request,
					HttpResponse.BodyHandlers.ofFile(temporaryFolder)
			);

			Path temporaryFilePath = response.body();

			String modId;
			try (
					JarFile jarFile = new JarFile(temporaryFilePath.toFile());
					InputStream fmjStream = getFmjAsStream(jarFile);
					InputStreamReader fmjStreamReader = new InputStreamReader(fmjStream)
			) {
				JsonElement potentialFmj = JsonParser.parseReader(fmjStreamReader);
				if (!potentialFmj.isJsonObject()) {
					throw new IllegalStateException("Attempted to get a non-JSONObject fabric.mod.json whilst getting project metadata.");
				}

				JsonObject fmj = potentialFmj.getAsJsonObject();

				modId = fmj.getAsJsonPrimitive("id").getAsString();
			}

			if (Files.deleteIfExists(temporaryFilePath)) {
				if (Files.isDirectory(temporaryFolder)) {
					try (var directoryStream = Files.newDirectoryStream(temporaryFolder)) {
						if (!directoryStream.iterator().hasNext()) {
							Files.deleteIfExists(temporaryFolder);
						}
					}
				}
			}

			return modId;
		} catch (IOException | InterruptedException e) {
			throw new HypertextException(500, e.getMessage());
		}
	}

	private static InputStream getFmjAsStream(JarFile file) throws HypertextException, IOException {
		ZipEntry entry = file.getEntry("fabric.mod.json");
		if (entry != null) {
			return file.getInputStream(entry);
		}
		throw new HypertextException(500, "The specified JAR is not a Fabric mod.");
	}

	public static <T> HttpResponse<T> get(String endpoint, HttpResponse.BodyHandler<T> bodyHandler, String... headers) throws IOException, InterruptedException {
		var req = HttpRequest.newBuilder(URI.create(API_URL + endpoint))
				.header("User-Agent", USER_AGENT);
		if (headers.length > 0) {
			req.headers(headers);
		}

		return HTTP_CLIENT.send(req.build(), bodyHandler);
	}

	public static <T> HttpResponse<T> post(String endpoint, HttpRequest.BodyPublisher bodyPublisher, HttpResponse.BodyHandler<T> bodyHandler, String... headers) throws IOException, InterruptedException {
		var req = HttpRequest.newBuilder(URI.create(API_URL + endpoint))
				.header("User-Agent", USER_AGENT);
		if (headers.length > 0) {
			req.headers(headers);
		}
		req.POST(bodyPublisher);

		return HTTP_CLIENT.send(req.build(), bodyHandler);
	}
}
