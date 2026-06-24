package net.modgarden.gardenbot.button;

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.modgarden.gardenbot.client.exception.HypertextException;
import net.modgarden.gardenbot.interaction.ButtonInteraction;
import net.modgarden.gardenbot.interaction.InteractionHandler;
import net.modgarden.gardenbot.response.Response;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public abstract class Button implements InteractionHandler<ButtonInteraction> {
	private final String id;
	private final String[] expectedArguments;

	public Button(String id, String... expectedArguments) {
		this.id = id;
		this.expectedArguments = expectedArguments;
	}

	public String id() {
		return id;
	}

	@NotNull
	public final Response respondInternal(ButtonInteractionEvent interaction) throws HypertextException {
		String fullId = Objects.requireNonNull(interaction.getButton().getId());
		return respondInternal(new ButtonInteraction(interaction, createArguments(fullId)));
	}

	public String withArguments(String... string) {
		if (expectedArguments.length == 0) {
			return id;
		}
		return id + "?" + String.join(",", string);
	}

	private Map<String, String> createArguments(String fullId) {
		Map<String, String> map = new HashMap<>();

		String argumentsOnly = fullId.substring(id.length() + "?".length());
		String[] arguments = argumentsOnly.split(",");

		if (expectedArguments.length != arguments.length) {
			throw new IllegalStateException("Invalid amount of arguments for modal '" + id + "'.\n" +
					"Requires fields: '" + String.join("', '", expectedArguments) + "'.");
		}

		for (int i = 0; i < expectedArguments.length; ++i) {
			String key = expectedArguments[i];
			String value = arguments[i];

			map.put(key, value);
		}

		return map;
	}
}
