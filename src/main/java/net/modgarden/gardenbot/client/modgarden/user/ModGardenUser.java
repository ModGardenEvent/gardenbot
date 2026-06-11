package net.modgarden.gardenbot.client.modgarden.user;

import java.util.List;

public record ModGardenUser(
		String id,
		String username,
		UserIntegrations integrations,
		List<String> events,
		List<String> projects,
		List<String> roles
) {
}
