package net.modgarden.gardenbot.client.exception;

import net.modgarden.gardenbot.GardenBot;
import net.modgarden.gardenbot.client.ModGarden;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.http.HttpResponse;

public class HypertextException extends Exception {
	private final int status;

	public HypertextException(int status, String message) {
		super(message);
		this.status = status;
	}

	public HypertextException(int status, Throwable cause) {
		super(cause);
		this.status = status;
	}

	public int getStatus() {
		return status;
	}

	public static HypertextException hypertextException(HttpResponse<InputStream> response) {
		return new HypertextException(response.statusCode(), GardenBot.GSON.fromJson(new InputStreamReader(response.body()), ModGarden.ExceptionPage.class).description());
	}
}
