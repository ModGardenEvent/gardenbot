package net.modgarden.gardenbot.client.modrinth;

import java.util.List;

public record ModrinthProject(String id,
                              String slug,
                              String title,
                              String published,
                              String updated,
                              List<String> versions) {
}
