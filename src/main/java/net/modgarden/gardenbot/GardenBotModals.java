package net.modgarden.gardenbot;

import net.modgarden.gardenbot.modals.account.LinkModrinthModal;
import net.modgarden.gardenbot.modals.account.RegisterModal;

import static net.modgarden.gardenbot.interaction.modal.ModalDispatcher.register;

public class GardenBotModals {
	public static final RegisterModal REGISTER = register(new RegisterModal());
	public static final LinkModrinthModal LINK_MODRINTH = register(new LinkModrinthModal());
}
