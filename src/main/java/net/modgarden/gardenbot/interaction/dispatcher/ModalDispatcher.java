package net.modgarden.gardenbot.interaction.dispatcher;

import net.modgarden.gardenbot.interaction.ModalInteraction;
import net.modgarden.gardenbot.response.Response;
import net.modgarden.gardenbot.modal.Modal;

import java.util.HashMap;

public class ModalDispatcher {
	private static final HashMap<String, Modal> MODALS = new HashMap<>();

	public static <T extends Modal> T register(T modal) {
		MODALS.put(modal.id, modal);
		return modal;
	}

	public static Response dispatch(ModalInteraction command) {
		var modal = MODALS.get(command.event().getModalId());
		return modal.respond(command);
	}
}
