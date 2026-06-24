package net.modgarden.gardenbot.client.mod_garden.event;

import net.modgarden.gardenbot.util.NullableWrapper;
import org.jetbrains.annotations.Nullable;

public record EventMetadata(String name, @Nullable String description) {
	public record Modifiable(@Nullable String name, @Nullable NullableWrapper<String> description) {
	}
}
