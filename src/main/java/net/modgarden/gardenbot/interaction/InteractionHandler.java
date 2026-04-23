package net.modgarden.gardenbot.interaction;

import net.modgarden.gardenbot.response.Response;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface InteractionHandler<TInteraction extends Interaction> {
	@NotNull
	Response respond(TInteraction interaction);
}
