package net.modgarden.gardenbot.client.modgarden.user;

import net.modgarden.gardenbot.client.modgarden.user.integration.DiscordUserIntegration;
import net.modgarden.gardenbot.client.modgarden.user.integration.MinecraftUserIntegration;
import net.modgarden.gardenbot.client.modgarden.user.integration.ModrinthUserIntegration;
import org.jetbrains.annotations.Nullable;

public record UserIntegrations(@Nullable DiscordUserIntegration discord,
							   @Nullable ModrinthUserIntegration modrinth,
							   @Nullable MinecraftUserIntegration minecraft) {
}
