package net.modgarden.gardenbot.interaction;

import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;

public record SlashCommandInteraction(GenericCommandInteractionEvent event) implements Interaction {

}
