package net.modgarden.gardenbot.interaction.response;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.InteractionCallbackAction;

public interface Response {
	InteractionCallbackAction<?> send(SlashCommandInteractionEvent event);
	InteractionCallbackAction<?> send(ButtonInteractionEvent event);
	InteractionCallbackAction<?> send(ModalInteractionEvent event);
}
