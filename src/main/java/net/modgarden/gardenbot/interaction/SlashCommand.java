package net.modgarden.gardenbot.interaction;

import org.jetbrains.annotations.NotNull;

public class SlashCommand {
	public final String NAME;
	public final InteractionHandler<SlashCommandInteraction> HANDLER;

	public SlashCommand(String name, InteractionHandler<SlashCommandInteraction> handler) {
		NAME = name;
		HANDLER = handler;
	}

	@NotNull
	public Response respond(SlashCommandInteraction interaction) {
		return HANDLER.respond(interaction);
	}
}
