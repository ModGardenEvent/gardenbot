package net.modgarden.gardenbot.interaction.dispatcher;

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.modgarden.gardenbot.button.Button;
import net.modgarden.gardenbot.client.exception.HypertextException;
import net.modgarden.gardenbot.database.DatabaseAccess;
import net.modgarden.gardenbot.response.Response;

import java.util.HashMap;
import java.util.Objects;
import java.util.function.Supplier;

public class ButtonDispatcher {
	private static final HashMap<String, Button> HANDLERS = new HashMap<>();

	public static <T extends Button> T register(Supplier<T> supplier) {
		T button = supplier.get();
		HANDLERS.put(button.id(), button);
		return button;
	}

	public static Response dispatch(ButtonInteractionEvent event) throws HypertextException {
		String id = Objects.requireNonNull(event.getButton().getId()).split("\\?")[0];
		return DatabaseAccess.bind().call(() -> HANDLERS.get(id).respondInternal(event));
	}
}
