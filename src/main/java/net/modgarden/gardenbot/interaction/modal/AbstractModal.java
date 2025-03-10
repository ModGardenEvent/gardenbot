package net.modgarden.gardenbot.interaction.modal;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.modgarden.gardenbot.interaction.InteractionHandler;
import net.modgarden.gardenbot.interaction.ModalInteraction;

public abstract class AbstractModal  implements InteractionHandler<ModalInteraction> {
	public final String ID;
	public final String TITLE;

	public AbstractModal(String id, String title) {
		ID = id;
		TITLE = title;
	}

	public abstract Modal getModal(SlashCommandInteractionEvent interaction);
}
