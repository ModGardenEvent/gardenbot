package net.modgarden.gardenbot.client.mod_garden.role;

import net.modgarden.gardenbot.client.mod_garden.role.integration.DiscordRoleIntegration;
import org.jetbrains.annotations.Nullable;

public record RoleIntegrations(@Nullable DiscordRoleIntegration discord) {
}
