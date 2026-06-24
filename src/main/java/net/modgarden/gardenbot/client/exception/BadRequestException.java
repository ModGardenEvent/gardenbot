package net.modgarden.gardenbot.client.exception;

public class BadRequestException extends HypertextException {
	public BadRequestException(String message) {
		super(400, message);
	}
}
