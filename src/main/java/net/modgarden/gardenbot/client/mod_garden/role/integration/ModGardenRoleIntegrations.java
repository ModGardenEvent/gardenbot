package net.modgarden.gardenbot.client.mod_garden.role.integration;

import net.modgarden.gardenbot.client.mod_garden.user.integration.DiscordUserIntegration;
import net.modgarden.gardenbot.client.mod_garden.user.integration.MinecraftUserIntegration;
import net.modgarden.gardenbot.client.mod_garden.user.integration.ModrinthUserIntegration;

public record ModGardenRoleIntegrations(ModrinthUserIntegration modrinth,
                                        DiscordUserIntegration discord,
                                        MinecraftUserIntegration minecraft) {
}
