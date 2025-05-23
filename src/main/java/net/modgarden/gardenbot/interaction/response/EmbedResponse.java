package net.modgarden.gardenbot.interaction.response;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.interactions.InteractionCallbackAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class EmbedResponse implements Response {
	private boolean ephemeral = false;
	private @Nullable String title = null;
	private @Nullable String description = null;
	private int color = -1;
	private final List<Button> buttons = new ArrayList<>();

	public MessageEmbed createEmbed() {
		var embed = new EmbedBuilder();

		if (title == null && description == null)
			throw new UnsupportedOperationException("Cannot create an embed without either a title or description.");

		if (title != null)
			embed.setTitle(title);
		if (description != null)
			embed.setDescription(description);
		if (color != -1)
			embed.setColor(color);

		return embed.build();
	}
	
	public RestAction<?> createAction(IReplyCallback callback) {
		if (callback.isAcknowledged()) {
			callback.getHook().editOriginalEmbeds(createEmbed()).queue();
			return callback.getHook().retrieveOriginal();
		}
		ReplyCallbackAction action = callback.replyEmbeds(createEmbed()).setEphemeral(isEphemeral());

		if (!buttons.isEmpty())
			action.addActionRow(buttons.toArray(Button[]::new));

		return action;
	}

	@Override
	public RestAction<?> send(SlashCommandInteractionEvent event) {
		return createAction(event);
	}

	@Override
	public RestAction<?> send(ButtonInteractionEvent event) {
		return createAction(event);
	}

	@Override
	public RestAction<?> send(ModalInteractionEvent event) {
		return createAction(event);
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

	public EmbedResponse setDescription(String description) {
		this.description = description;

		return this;
	}

	public EmbedResponse setColor(int color) {
		this.color = color;

		return this;
	}

	public EmbedResponse addButton(String id, String label, ButtonStyle style) {
		return addButton(id, label, style, null);
	}

	public EmbedResponse addButton(String id, String label, ButtonStyle style, @Nullable Emoji emoji) {
		buttons.add(Button.of(style, id, label, emoji));

		return this;
	}

	public EmbedResponse addButtonUrl(URI url, String label) {
		return addButtonUrl(url, label, null);
	}

	public EmbedResponse addButtonUrl(URI url, String label, @Nullable Emoji emoji) {
		buttons.add(Button.of(ButtonStyle.LINK, url.toString(), label, emoji));

		return this;
	}

}
