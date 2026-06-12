package net.modgarden.gardenbot.client.mod_garden.event;

public record ModGardenEvent(String id,
                             String slug,
                             EventMetadata metadata,
                             EventTimes times,
                             EventPlatform platform,
                             EventRoles roles) {
}
