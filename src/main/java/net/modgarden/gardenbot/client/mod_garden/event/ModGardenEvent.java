package net.modgarden.gardenbot.client.mod_garden.event;

import java.util.Map;

import org.jetbrains.annotations.Nullable;

public record ModGardenEvent(String id,
                             String slug,
                             EventMetadata metadata,
                             EventTimes times,
                             EventPlatform platform,
                             Map<String, String> roles) {
	public @Nullable String getParticipantRole() {
		return roles.get("participant");
	}
}
