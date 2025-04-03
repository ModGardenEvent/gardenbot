package net.modgarden.gardenbot.interaction.button;

import net.modgarden.gardenbot.interaction.ButtonInteraction;
import net.modgarden.gardenbot.interaction.response.Response;

import java.awt.desktop.PrintFilesEvent;
import java.util.HashMap;
import java.util.function.Function;

public class ButtonDispatcher {
	private static final HashMap<String, Response> HANDLERS = new HashMap<>();

	public static void register(String buttonId, Response response) {
		HANDLERS.put(buttonId, response);
	}

	public static Response dispatch(ButtonInteraction interaction) {
		return HANDLERS.get(interaction.event().getButton().getId());
	}
}
