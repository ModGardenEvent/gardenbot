package net.modgarden.gardenbot.interaction.dispatcher;

import net.modgarden.gardenbot.database.DatabaseAccess;
import net.modgarden.gardenbot.interaction.ModalInteraction;
import net.modgarden.gardenbot.response.Response;
import net.modgarden.gardenbot.modal.Modal;

import java.util.HashMap;
import java.util.function.Supplier;

public class ModalDispatcher {
	private static final HashMap<String, Modal> MODALS = new HashMap<>();

	public static <T extends Modal> T register(Supplier<T> supplier) {
		T modal = supplier.get();
		MODALS.put(modal.id, modal);
		return modal;
	}

	public static Response dispatch(ModalInteraction command) {
		return DatabaseAccess.bind().call(() -> MODALS.get(command.event().getModalId()).respond(command));
	}
}
