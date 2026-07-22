package net.modgarden.gardenbot.client.mod_garden.project.patch;

import java.util.Map;

public record ProjectTeamPatch(Map<String, String> team, Map<String, String> permissions) {
}
