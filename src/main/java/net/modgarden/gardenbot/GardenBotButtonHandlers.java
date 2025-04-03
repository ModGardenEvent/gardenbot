package net.modgarden.gardenbot;

import net.modgarden.gardenbot.interaction.button.ButtonDispatcher;
import net.modgarden.gardenbot.interaction.response.ModalResponse;

public class GardenBotButtonHandlers {
	public static void registerAll() {
		ButtonDispatcher.register("modalLinkModrinth", new ModalResponse(GardenBotModals.LINK_MODRINTH));
	}
}
