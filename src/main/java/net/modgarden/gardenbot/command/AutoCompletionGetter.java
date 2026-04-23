package net.modgarden.gardenbot.command;

import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

@FunctionalInterface
	public interface AutoCompletionGetter {
		<T> T getOption(String name,
						@Nullable T fallback,
						@NotNull Function<? super OptionMapping, ? extends T> resolver);

		default <T> T getOption(String name,
								@NotNull Function<? super OptionMapping, ? extends T> resolver) {
			return getOption(name, null, resolver);
		}
	}
