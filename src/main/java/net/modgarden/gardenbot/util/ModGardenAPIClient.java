package net.modgarden.gardenbot.util;

import net.modgarden.gardenbot.GardenBot;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ModGardenAPIClient {
	private static final String USER_AGENT = "ModGardenEvent/gardenbot/" + GardenBot.VERSION + " (modgarden.net)";

	public static <T> HttpResponse<T> get(String endpoint, HttpResponse.BodyHandler<T> bodyHandler, String... headers) throws IOException, InterruptedException {
		var req = HttpRequest.newBuilder(URI.create(GardenBot.API_URL + endpoint))
				.header("User-Agent", USER_AGENT)
				.header("Authorization", "Basic " + GardenBot.DOTENV.get("OAUTH_SECRET"));
		if (headers.length > 0)
			req.headers(headers);

		return GardenBot.HTTP_CLIENT.send(req.build(), bodyHandler);
	}

	public static <T> HttpResponse<T> post(String endpoint, HttpRequest.BodyPublisher bodyPublisher, HttpResponse.BodyHandler<T> bodyHandler, String... headers) throws IOException, InterruptedException {
		var req = HttpRequest.newBuilder(URI.create(GardenBot.API_URL + endpoint))
				.header("User-Agent", USER_AGENT)
				.header("Authorization", "Basic " + GardenBot.DOTENV.get("OAUTH_SECRET"));
		if (headers.length > 0)
			req.headers(headers);
		req.POST(bodyPublisher);

		return GardenBot.HTTP_CLIENT.send(req.build(), bodyHandler);
	}
}
