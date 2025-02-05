package net.modgarden.gardenbot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.modgarden.gardenbot.interaction.SlashCommandInteraction;
import net.modgarden.gardenbot.interaction.command.SlashCommandDispatcher;
import org.jetbrains.annotations.NotNull;

public class GardenBotEvents extends ListenerAdapter {
	@Override
	public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
		var response = SlashCommandDispatcher.dispatch(new SlashCommandInteraction(event));

		event.replyEmbeds(new EmbedBuilder()
						.setTitle(response.getTitle())
						.setDescription(response.getDescription())
						.build()
		).setEphemeral(response.isEphemeral()).queue();
	}
}
