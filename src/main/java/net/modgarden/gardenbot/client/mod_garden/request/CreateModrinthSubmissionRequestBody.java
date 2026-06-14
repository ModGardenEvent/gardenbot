package net.modgarden.gardenbot.client.mod_garden.request;

import com.google.gson.annotations.SerializedName;
import net.modgarden.gardenbot.client.mod_garden.project.ModrinthSubmissionPlatform;

public record CreateModrinthSubmissionRequestBody(@SerializedName("project_id") String projectId,
                                                  @SerializedName("event_id") String eventId,
                                                  ModrinthSubmissionPlatform platform) {
}
