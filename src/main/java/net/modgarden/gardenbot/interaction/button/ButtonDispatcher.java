package net.modgarden.gardenbot.interaction.button;

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
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

	public static Response dispatch(ButtonInteractionEvent event) {
		String id = event.getButton().getId();
		String[] arguments = new String[0];
		if (id != null) {
			String finalId = id;
			var nonArgumentButton = HANDLERS.keySet().stream()
					.filter(s -> s.contains("?") && finalId.startsWith(s.split("\\?")[0]))
					.findAny();
			if (nonArgumentButton.isPresent()) {
				arguments = id.split("\\?")[1].split(",");
				if (arguments.length != nonArgumentButton.get().split("\\?")[1].split(",").length) {
					throw new IllegalArgumentException("Incorrect number of arguments for the specified button.");
				}
				id = nonArgumentButton.get();
			}
		}
		return HANDLERS.get(id).apply(new ButtonInteraction(event, arguments));
	}
}
