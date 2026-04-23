package net.modgarden.gardenbot.response;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.requests.RestAction;

public interface Response {
	default RestAction<?> reply(IReplyCallback callback) {
		throw new RuntimeException("A response for this interaction is not implemented");
	}

	default RestAction<?> replyToSlashCommand(SlashCommandInteractionEvent event) {
		return reply(event);
	}

	default RestAction<?> replyToButton(ButtonInteractionEvent event) {
		return reply(event);
	}

	default RestAction<?> replyToModal(ModalInteractionEvent event) {
		return reply(event);
	}
}
