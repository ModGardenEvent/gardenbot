package net.modgarden.gardenbot.client.mod_garden.request;

import com.google.gson.annotations.SerializedName;
import net.modgarden.gardenbot.client.mod_garden.project.SubmissionPlatform;

public record CreateSubmissionModrinthRequestBody(@SerializedName("project_id") String projectId,
                                                  @SerializedName("event_id") String eventId,
                                                  SubmissionPlatform platform) {
}
