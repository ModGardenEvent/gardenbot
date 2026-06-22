package net.modgarden.gardenbot;

import net.modgarden.gardenbot.modal.account.RegisterModal;

import static net.modgarden.gardenbot.interaction.dispatcher.ModalDispatcher.register;

public class GardenBotModals {
	public static final RegisterModal REGISTER = register(RegisterModal::new);

	public static void registerAll() {
	}
}
