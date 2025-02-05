package net.modgarden.gardenbot.interaction;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public record SlashCommandInteraction(SlashCommandInteractionEvent event) implements Interaction {
}
