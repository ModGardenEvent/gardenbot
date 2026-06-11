package net.modgarden.gardenbot.client.modgarden.event;

import java.util.List;

public record ModGardenGenre(String id,
                             String slug,
                             EventMetadata metadata,
                             List<String> events) {
}
