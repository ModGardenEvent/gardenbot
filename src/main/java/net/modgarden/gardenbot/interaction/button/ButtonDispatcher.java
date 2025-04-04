package net.modgarden.gardenbot.interaction.button;

import net.modgarden.gardenbot.interaction.ButtonInteraction;
import net.modgarden.gardenbot.interaction.response.Response;

import java.util.HashMap;
import java.util.function.Function;

public class ButtonDispatcher {
	private static final HashMap<String, Function<ButtonInteraction, Response>> HANDLERS = new HashMap<>();

	public static void register(String buttonId, Function<ButtonInteraction, Response> function) {
		HANDLERS.put(buttonId, function);
	}

	public static void register(String buttonId, Response response) {
		HANDLERS.put(buttonId, buttonInteraction -> response);
	}

	public static Response dispatch(ButtonInteraction interaction) {
		return HANDLERS.get(interaction.event().getButton().getId()).apply(interaction);
	}
}
