package net.modgarden.gardenbot.client.mod_garden.request;

import net.modgarden.gardenbot.client.mod_garden.project.ProjectMetadata;

public record CreateProjectRequestBody(ProjectMetadata metadata) {
}
