package net.modgarden.gardenbot;

import net.modgarden.gardenbot.modal.account.link.LinkMinecraftModal;
import net.modgarden.gardenbot.modal.account.link.LinkModrinthModal;
import net.modgarden.gardenbot.modal.account.RegisterModal;

import static net.modgarden.gardenbot.interaction.dispatcher.ModalDispatcher.register;

public class GardenBotModals {
	public static final RegisterModal REGISTER = register(new RegisterModal());
	public static final LinkModrinthModal LINK_MODRINTH = register(new LinkModrinthModal());
	public static final LinkMinecraftModal LINK_MINECRAFT = register(new LinkMinecraftModal());
}
