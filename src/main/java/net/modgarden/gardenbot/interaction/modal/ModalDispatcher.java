package net.modgarden.gardenbot.interaction.modal;

import net.modgarden.gardenbot.interaction.ModalInteraction;
import net.modgarden.gardenbot.interaction.response.Response;

import java.util.HashMap;

public class ModalDispatcher {
	private static final HashMap<String, AbstractModal> MODALS = new HashMap<>();

	public static <T extends AbstractModal> T register(T modal) {
		MODALS.put(modal.ID, modal);
		return modal;
	}

	public static Response dispatch(ModalInteraction command) {
		var modal = MODALS.get(command.event().getModalId());
		return modal.respond(command);
	}
}
