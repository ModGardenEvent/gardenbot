package net.modgarden.gardenbot.interaction;

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

public record ButtonInteraction(ButtonInteractionEvent event) implements Interaction {

}
