package net.modgarden.gardenbot.util;

import com.google.gson.*;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;

/// Used to allow serialization of {@link com.google.gson.JsonNull} within {@link net.modgarden.gardenbot.GardenBot#GSON}.
public record NullableWrapper<T>(@Nullable T value) {
	public static <T> NullableWrapper<T> of(T value) {
		return new NullableWrapper<>(value);
	}

	public static <T> NullableWrapper<T> empty() {
		return new NullableWrapper<>(null);
	}

	public boolean isPresent() {
		return value != null;
	}

	public boolean isEmpty() {
		return value == null;
	}

	public static class Serializer implements JsonSerializer<NullableWrapper<?>> {
		@Override
		public JsonElement serialize(NullableWrapper<?> src, Type typeOfSrc, JsonSerializationContext context) {
			if (src.value() == null) {
				return JsonNull.INSTANCE;
			}
			return context.serialize(src.value(), src.value().getClass());
		}
	}
}
