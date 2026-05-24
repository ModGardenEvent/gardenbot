package net.modgarden.gardenbot.interaction;

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import java.util.Map;

public record ButtonInteraction(ButtonInteractionEvent event, Map<String, String> arguments) implements Interaction {

}
