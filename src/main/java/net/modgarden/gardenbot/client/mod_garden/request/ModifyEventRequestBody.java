package net.modgarden.gardenbot.client.mod_garden.request;

import java.util.Map;

import net.modgarden.gardenbot.client.mod_garden.event.EventMetadata;
import net.modgarden.gardenbot.client.mod_garden.event.EventPlatform;
import net.modgarden.gardenbot.client.mod_garden.event.EventTimes;
import net.modgarden.gardenbot.util.NullableWrapper;
import org.jetbrains.annotations.Nullable;

public record ModifyEventRequestBody(
		@Nullable EventMetadata.Modifiable metadata,
		@Nullable EventTimes.Modifiable times,
		@Nullable EventPlatform.Modifiable platform,
		Map<String, NullableWrapper<String>> roles
) {
}
