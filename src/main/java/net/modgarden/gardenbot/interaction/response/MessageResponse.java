package net.modgarden.gardenbot.interaction.response;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.interactions.InteractionCallbackAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.jetbrains.annotations.Nullable;

public class MessageResponse implements Response {
	private boolean ephemeral = false;
	private @Nullable String message;

	public RestAction<?> reply(IReplyCallback callback) {
		if (callback.isAcknowledged()) {
			callback.getHook().editOriginal(getMessage()).queue();
			return callback.getHook().retrieveOriginal();
		}

		return callback.reply(getMessage()).setEphemeral(isEphemeral());
	}

	@Override
	public RestAction<?> send(SlashCommandInteractionEvent event) {
		return reply(event);
	}

	@Override
	public RestAction<?> send(ButtonInteractionEvent event) {
		return reply(event);
	}

	@Override
	public RestAction<?> send(ModalInteractionEvent event) {
		return reply(event);
	}

	public MessageResponse markEphemeral(boolean ephemeral) {
		this.ephemeral = ephemeral;

		return this;
	}

	public MessageResponse markEphemeral() {
		return markEphemeral(true);
	}

	public boolean isEphemeral() {
		return ephemeral;
	}

	public MessageResponse setMessage(String message) {
		this.message = message;

		return this;
	}

	public String getMessage() {
		if (message == null)
			throw new NullPointerException("Message cannot be null before sending");

		return message;
	}
}
