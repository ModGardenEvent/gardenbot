package net.modgarden.gardenbot.interaction.response;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.InteractionCallbackAction;
import net.modgarden.gardenbot.interaction.modal.AbstractModal;

public class ModalResponse implements Response {
	private final AbstractModal MODAL;

	public ModalResponse(AbstractModal modal) {
		this.MODAL = modal;
	}

	@Override
	public InteractionCallbackAction<?> send(SlashCommandInteractionEvent event) {
		return event.replyModal(MODAL.getModal(event));
	}

	@Override
	public InteractionCallbackAction<?> send(ButtonInteractionEvent event) {
		return event.replyModal(MODAL.getModal(event));
	}

	@Override
	public InteractionCallbackAction<?> send(ModalInteractionEvent event) {
		throw new UnsupportedOperationException("Sending a modal from a modal is unsupported.");
	}
}
