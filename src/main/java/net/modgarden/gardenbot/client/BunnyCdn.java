package net.modgarden.gardenbot.client;

import net.modgarden.gardenbot.GardenBot;
import net.modgarden.gardenbot.client.exception.HypertextException;
import net.modgarden.gardenbot.client.exception.InternalServerException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static net.modgarden.gardenbot.GardenBot.HTTP_CLIENT;

@SuppressWarnings("UastIncorrectHttpHeaderInspection")
public class BunnyCdn {
	public static final String API_URL = "https://ny.storage.bunnycdn.com/mod-garden/";

	public static boolean fileExists(String location) throws HypertextException {
		HttpResponse<Void> response;
		try {
			response = get(location, HttpResponse.BodyHandlers.discarding());
		} catch (IOException | InterruptedException e) {
			throw new InternalServerException(e.getMessage());
		}
		return response.statusCode() != 404;
	}

	public static void upload(String fileName, InputStream attachmentStream) throws HypertextException {
		try {
			BunnyCdn.put(
					fileName,
					HttpRequest.BodyPublishers.ofInputStream(() -> attachmentStream),
					HttpResponse.BodyHandlers.ofInputStream(),
					"Content-Type", "application/octet-stream",
					"Accept", "image/png"
			);
		} catch (IOException | InterruptedException e) {
			throw new InternalServerException(e.getMessage());
		}
	}

	public static <T> HttpResponse<T> get(String endpoint, HttpResponse.BodyHandler<T> bodyHandler, String... headers) throws IOException, InterruptedException {
		var req = HttpRequest.newBuilder(URI.create(API_URL + endpoint))
				.header("AccessKey", GardenBot.DOTENV.get("BUNNY_CDN_KEY"));
		if (headers.length > 0)
			req.headers(headers);

		return HTTP_CLIENT.send(req.build(), bodyHandler);
	}

	public static <T> HttpResponse<T> put(String endpoint, HttpRequest.BodyPublisher bodyPublisher, HttpResponse.BodyHandler<T> bodyHandler, String... headers) throws IOException, InterruptedException {
		var req = HttpRequest.newBuilder(URI.create(API_URL + endpoint))
				.header("AccessKey", GardenBot.DOTENV.get("BUNNY_CDN_KEY"));
		if (headers.length > 0)
			req.headers(headers);
		req.PUT(bodyPublisher);

		return HTTP_CLIENT.send(req.build(), bodyHandler);
	}
}
