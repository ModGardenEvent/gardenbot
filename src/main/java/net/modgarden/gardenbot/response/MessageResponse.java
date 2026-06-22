package net.modgarden.gardenbot.response;

import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.requests.RestAction;

public class MessageResponse implements Response {
	private final String message;
	private boolean ephemeral = false;

	public MessageResponse(String message) {
		this.message = message;
	}

	@Override
	public RestAction<?> reply(IReplyCallback callback) {
		if (callback.isAcknowledged()) {
			callback.getHook().editOriginal(message).queue();
			return callback.getHook().retrieveOriginal();
		}

		return callback.reply(message).setEphemeral(ephemeral);
	}

	public MessageResponse markEphemeral() {
		ephemeral = true;
		return this;
	}

	public boolean isEphemeral() {
		return ephemeral;
	}
}
