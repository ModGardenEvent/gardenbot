package net.modgarden.gardenbot.client.mod_garden.request;

import net.modgarden.gardenbot.client.mod_garden.project.ModrinthSubmissionPlatform;

public record CreateModrinthSubmissionRequestBody(String projectId,
                                                  String eventId,
                                                  ModrinthSubmissionPlatform platform) {
}
