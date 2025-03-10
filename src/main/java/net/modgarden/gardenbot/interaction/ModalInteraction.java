package net.modgarden.gardenbot.interaction;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;

public record ModalInteraction(ModalInteractionEvent event) implements Interaction {

}
