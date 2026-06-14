package net.modgarden.gardenbot.client.mod_garden.project;

import com.google.gson.annotations.SerializedName;

public record ModGardenSubmission(String id,
                                  @SerializedName("event_id") String eventId,
                                  ModGardenProject project) {
}
