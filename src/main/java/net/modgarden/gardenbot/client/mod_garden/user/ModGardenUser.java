package net.modgarden.gardenbot.client.mod_garden.user;

import java.util.List;

public record ModGardenUser(
		String id,
		String username,
		String permissions,
		List<String> events,
		List<String> projects,
		List<String> roles,
		UserIntegrations integrations
) {
}
