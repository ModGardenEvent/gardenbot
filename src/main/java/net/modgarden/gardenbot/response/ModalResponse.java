package net.modgarden.gardenbot.response;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.requests.RestAction;
import net.modgarden.gardenbot.modal.Modal;

public class ModalResponse implements Response {
	private final Response reply;
	private final Modal modal;

	public ModalResponse(Response reply, Modal modal) {
		this.reply = reply;
		this.modal = modal;
	}

	@Override
	public RestAction<?> replyToSlashCommand(SlashCommandInteractionEvent event) {
		reply.reply(event).queue();
		return event.replyModal(modal.getModal());
	}

	@Override
	public RestAction<?> replyToButton(ButtonInteractionEvent event) {
		return event.replyModal(modal.getModal());
	}

	@Override
	public RestAction<?> replyToModal(ModalInteractionEvent event) {
		throw new UnsupportedOperationException("Sending a modal from a modal is unsupported.");
	}
}
