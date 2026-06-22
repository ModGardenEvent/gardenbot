package net.modgarden.gardenbot.interaction;

import net.modgarden.gardenbot.client.exception.HypertextException;
import net.modgarden.gardenbot.response.EmbedResponse;
import net.modgarden.gardenbot.response.Response;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

@FunctionalInterface
public interface InteractionHandler<TInteraction extends Interaction> {
	default Response respondInternal(TInteraction interaction) {
		try {
			return respond(interaction);
		} catch (HypertextException e) {
			return exceptionResponse(e.getStatus() + ": " + e.getMessage());
		} catch (SQLException e) {
			return exceptionResponse(e.getMessage());
		}
	}

	@NotNull
	Response respond(TInteraction interaction) throws HypertextException, SQLException;

	default EmbedResponse exceptionResponse(HypertextException exception) {
		return exceptionResponse(exception.getStatus() + ": " + exception.getMessage());
	}

	default EmbedResponse exceptionResponse(String message) {
		return new EmbedResponse()
				.setTitle("Encountered an exception!")
				.setDescription(message + "\nPlease report this to a team member.")
				.markEphemeral()
				.setColor(0xFF0000);
	}
}
