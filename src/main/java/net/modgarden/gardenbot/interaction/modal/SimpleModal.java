package net.modgarden.gardenbot.interaction.modal;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.modgarden.gardenbot.interaction.InteractionHandler;
import net.modgarden.gardenbot.interaction.ModalInteraction;
import net.modgarden.gardenbot.interaction.response.Response;
import org.jetbrains.annotations.NotNull;

public class SimpleModal extends AbstractModal {
	private final InteractionHandler<ModalInteraction> HANDLER;
	public final LayoutComponent[] COMPONENTS;

	public SimpleModal(String id, String title, InteractionHandler<ModalInteraction> handler, LayoutComponent... components) {
		super(id, title);
		HANDLER = handler;
		COMPONENTS = components;
	}

	@NotNull
	@Override
	public Response respond(ModalInteraction interaction) {
		return HANDLER.respond(interaction);
	}

	@Override
	public Modal getModal(SlashCommandInteractionEvent interaction) {
		return Modal.create(ID, TITLE)
				.addComponents(COMPONENTS).build();
	}
}
