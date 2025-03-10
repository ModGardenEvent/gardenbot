package net.modgarden.gardenbot;

import net.modgarden.gardenbot.interaction.modal.RegisterModal;
import net.modgarden.gardenbot.interaction.modal.SimpleModal;

import static net.modgarden.gardenbot.interaction.modal.ModalDispatcher.register;

public class GardenBotModals {
	public static final SimpleModal REGISTER = register(new RegisterModal());
}
