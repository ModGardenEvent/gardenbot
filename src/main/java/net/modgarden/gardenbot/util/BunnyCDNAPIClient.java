package net.modgarden.gardenbot.util;

import net.modgarden.gardenbot.GardenBot;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class BunnyCDNAPIClient {
	public static final String API_URL = "https://ny.storage.bunnycdn.com/mod-garden/";

	public static <T> HttpResponse<T> get(String endpoint, HttpResponse.BodyHandler<T> bodyHandler, String... headers) throws IOException, InterruptedException {
		var req = HttpRequest.newBuilder(URI.create(API_URL + endpoint))
				.header("AccessKey", GardenBot.DOTENV.get("BUNNY_CDN_KEY"));
		if (headers.length > 0)
			req.headers(headers);

		return GardenBot.HTTP_CLIENT.send(req.build(), bodyHandler);
	}

	public static <T> HttpResponse<T> post(String endpoint, HttpRequest.BodyPublisher bodyPublisher, HttpResponse.BodyHandler<T> bodyHandler, String... headers) throws IOException, InterruptedException {
		var req = HttpRequest.newBuilder(URI.create(API_URL + endpoint))
				.header("AccessKey", GardenBot.DOTENV.get("BUNNY_CDN_KEY"));
		if (headers.length > 0)
			req.headers(headers);
		req.POST(bodyPublisher);

		return GardenBot.HTTP_CLIENT.send(req.build(), bodyHandler);
	}

	public static <T> HttpResponse<T> put(String endpoint, HttpRequest.BodyPublisher bodyPublisher, HttpResponse.BodyHandler<T> bodyHandler, String... headers) throws IOException, InterruptedException {
		var req = HttpRequest.newBuilder(URI.create(API_URL + endpoint))
				.header("AccessKey", GardenBot.DOTENV.get("BUNNY_CDN_KEY"));
		if (headers.length > 0)
			req.headers(headers);
		req.PUT(bodyPublisher);

		return GardenBot.HTTP_CLIENT.send(req.build(), bodyHandler);
	}
}
