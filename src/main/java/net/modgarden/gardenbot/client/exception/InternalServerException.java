package net.modgarden.gardenbot.client.exception;

public class InternalServerException extends HypertextException {
	public InternalServerException(String message) {
		super(500, message);
	}

	public InternalServerException(Throwable cause) {
		super(500, cause);
	}
}
