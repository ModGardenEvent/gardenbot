package net.modgarden.gardenbot.interaction.response;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.InteractionCallbackAction;
import org.jetbrains.annotations.Nullable;

public class MessageResponse implements Response {
	private boolean ephemeral = false;
	private @Nullable String message;


	@Override
	public InteractionCallbackAction<?> send(SlashCommandInteractionEvent event) {
		return event.reply(getMessage()).setEphemeral(isEphemeral());
	}

	@Override
	public InteractionCallbackAction<?> send(ButtonInteractionEvent event) {
		return event.reply(getMessage()).setEphemeral(isEphemeral());
	}

	@Override
	public InteractionCallbackAction<?> send(ModalInteractionEvent event) {
		return event.reply(getMessage()).setEphemeral(isEphemeral());
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
