package net.modgarden.gardenbot.interaction.response;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.InteractionCallbackAction;
import org.jetbrains.annotations.Nullable;

public class EmbedResponse implements Response {
	private boolean ephemeral = false;
	private @Nullable String title = null;
	private @Nullable String description = null;


	@Override
	public InteractionCallbackAction<?> send(SlashCommandInteractionEvent event) {
		return event.replyEmbeds(new EmbedBuilder()
				.setTitle(getTitle())
				.setDescription(getDescription())
				.build()
		).setEphemeral(isEphemeral());
	}

	@Override
	public InteractionCallbackAction<?> send(ModalInteractionEvent event) {
		return event.replyEmbeds(new EmbedBuilder()
				.setTitle(getTitle())
				.setDescription(getDescription())
				.build()
		).setEphemeral(isEphemeral());
	}

	public EmbedResponse markEphemeral(boolean ephemeral) {
		this.ephemeral = ephemeral;

		return this;
	}

	public EmbedResponse markEphemeral() {
		return markEphemeral(true);
	}

	public boolean isEphemeral() {
		return ephemeral;
	}

	public EmbedResponse setTitle(String title) {
		this.title = title;

		return this;
	}

	public String getTitle() {
		if (title == null)
			throw new NullPointerException("Title cannot be null before sending");

		return title;
	}

	public EmbedResponse setDescription(String description) {
		this.description = description;

		return this;
	}

	public String getDescription() {
		if (description == null)
			throw new NullPointerException("Description cannot be null before sending");

		return description;
	}
}
