package net.modgarden.gardenbot.client.mod_garden.role;

import org.jetbrains.annotations.Nullable;

public record ModGardenRole(String name,
							String permissions,
							String created,
                            String id,
                            RoleIntegrations integrations) {
	public record Modifiable(
			@Nullable String name,
			@Nullable String permissions,
			@Nullable RoleIntegrations.Modifiable integrations
	) {
	}
}
