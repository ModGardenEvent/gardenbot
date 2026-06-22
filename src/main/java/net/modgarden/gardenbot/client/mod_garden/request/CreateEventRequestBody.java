package net.modgarden.gardenbot.client.mod_garden.request;

import net.modgarden.gardenbot.client.mod_garden.event.EventMetadata;
import net.modgarden.gardenbot.client.mod_garden.event.EventPlatform;
import net.modgarden.gardenbot.client.mod_garden.event.EventTimes;

public record CreateEventRequestBody(String genre,
                                     String slug,
                                     EventMetadata metadata,
                                     EventTimes times,
                                     EventPlatform platform) {

}
