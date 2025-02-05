package net.modgarden.gardenbot.interaction;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface InteractionHandler<TInteraction extends Interaction> {
	@NotNull
	Response respond(TInteraction interaction);
}
