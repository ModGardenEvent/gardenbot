package net.modgarden.gardenbot.interaction.response;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.requests.RestAction;

public interface Response {
	RestAction<?> send(SlashCommandInteractionEvent event);
	RestAction<?> send(ButtonInteractionEvent event);
	RestAction<?> send(ModalInteractionEvent event);
}
