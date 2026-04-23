package net.modgarden.gardenbot.modal;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.modgarden.gardenbot.interaction.InteractionHandler;
import net.modgarden.gardenbot.interaction.ModalInteraction;

public abstract class Modal implements InteractionHandler<ModalInteraction> {
	public final String id;
	private final String title;
	private final LayoutComponent[] components;

	public Modal(String id, String title, LayoutComponent... components) {
		this.id = id;
		this.title = title;
		this.components = components;
	}

	public net.dv8tion.jda.api.interactions.modals.Modal getModal() {
		return net.dv8tion.jda.api.interactions.modals.Modal.create(id, title)
				.addComponents(components)
				.build();
	}
}
