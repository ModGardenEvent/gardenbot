package net.modgarden.gardenbot.client.modgarden.project;

import java.util.List;
import java.util.Map;

public record ModGardenProject(String id,
                               ProjectMetadata metadata,
							   Map<String, String> team,
                               Map<String, String> permissions,
                               List<String> submissions) {
}
