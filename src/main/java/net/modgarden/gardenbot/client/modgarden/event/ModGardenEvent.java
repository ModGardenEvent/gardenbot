package net.modgarden.gardenbot.client.modgarden.event;

public record ModGardenEvent(String id,
                             String slug,
							 EventMetadata metadata,
                             EventTimes times,
							 EventRoles roles) {
}
