package net.modgarden.gardenbot.util;

import net.modgarden.gardenbot.GardenBot;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/// Copied from the backend.
public class FileUtils {
	private static final String USER_AGENT = "ModGardenEvent/gardenbot/" + GardenBot.VERSION + " (modgarden.net)";

	public static File download(URI uri) throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder()
				.header("User-Agent", USER_AGENT)
				.uri(uri)
				.build();
		String fileName = getFileName(uri);

		Path temporaryFolder = Path.of("./.tmp")
				.resolve(fileName);

		Files.createDirectories(temporaryFolder.getParent());

		HttpResponse<Path> response = GardenBot.HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofFile(temporaryFolder));

		return response.body().toFile();
	}

	public static boolean isJar(File file) {
		String fileName = file.getName();

		int i = fileName.lastIndexOf('.');

		if (i > 0) {
			String fileExtension = fileName.substring(i + 1);
			return fileExtension.equals("jar");
		}

		return false;
	}

	public static void cleanupTmpFolder(File temporaryFile) {
		try {
			Path temporaryFilePath = temporaryFile.toPath();
			if (Files.deleteIfExists(temporaryFilePath)) {
				Path temporaryFolder = temporaryFilePath.getParent();
				if (Files.isDirectory(temporaryFilePath)) {
					try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(temporaryFolder)) {
						if (!directoryStream.iterator().hasNext()) {
							Files.deleteIfExists(temporaryFolder);
						}
					}
				}
			}
		} catch (IOException e) {
			throw new InternalError("Failed to clean-up temporary folder for checking the submitted JAR.");
		}
	}

	private static String getFileName(URI uri) {
		return Paths.get(uri.getPath()).getFileName().toString();
	}
}
