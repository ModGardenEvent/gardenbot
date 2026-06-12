package net.modgarden.gardenbot.client.mod_garden.user;

import net.modgarden.gardenbot.client.mod_garden.user.integration.DiscordUserIntegration;
import net.modgarden.gardenbot.client.mod_garden.user.integration.MinecraftUserIntegration;
import net.modgarden.gardenbot.client.mod_garden.user.integration.ModrinthUserIntegration;
import org.jetbrains.annotations.Nullable;

public record UserIntegrations(@Nullable DiscordUserIntegration discord,
                               @Nullable ModrinthUserIntegration modrinth,
                               @Nullable MinecraftUserIntegration minecraft) {
}
