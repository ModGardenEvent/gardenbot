package net.modgarden.gardenbot.client.modgarden.role.integration;

import net.modgarden.gardenbot.client.modgarden.user.integration.DiscordUserIntegration;
import net.modgarden.gardenbot.client.modgarden.user.integration.MinecraftUserIntegration;
import net.modgarden.gardenbot.client.modgarden.user.integration.ModrinthUserIntegration;

public record ModGardenRoleIntegrations(ModrinthUserIntegration modrinth,
                                        DiscordUserIntegration discord,
                                        MinecraftUserIntegration minecraft) {
}
