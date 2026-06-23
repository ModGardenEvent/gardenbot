package net.modgarden.gardenbot.client.mod_garden.role;

import net.modgarden.gardenbot.client.mod_garden.role.integration.DiscordRoleIntegration;
import net.modgarden.gardenbot.util.NullableWrapper;
import org.jetbrains.annotations.Nullable;

public record RoleIntegrations(@Nullable DiscordRoleIntegration discord) {
	public record Modifiable(@Nullable NullableWrapper<DiscordRoleIntegration> discord) {
	}
}
