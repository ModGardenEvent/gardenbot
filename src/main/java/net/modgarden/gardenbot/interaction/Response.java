package net.modgarden.gardenbot.interaction;

import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unchecked")
public class Response {
	private boolean ephemeral = false;
	private @Nullable String title = null;
	private @Nullable String description = null;

	public Response markEphemeral(boolean ephemeral) {
		this.ephemeral = ephemeral;

		return this;
	}

	public Response markEphemeral() {
		ephemeral = true;

		return this;
	}

	public boolean isEphemeral() {
		return ephemeral;
	}

	public Response setTitle(String title) {
		this.title = title;

		return this;
	}

	public String getTitle() {
		if (title == null)
			throw new NullPointerException("Title cannot be null before sending");

		return title;
	}

	public Response setDescription(String description) {
		this.description = description;

		return this;
	}

	public String getDescription() {
		if (description == null)
			throw new NullPointerException("Description cannot be null before sending");

		return description;
	}
}
