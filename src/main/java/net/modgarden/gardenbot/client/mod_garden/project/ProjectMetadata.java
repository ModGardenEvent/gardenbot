package net.modgarden.gardenbot.client.mod_garden.project;

import com.google.gson.annotations.SerializedName;

public record ProjectMetadata(@SerializedName("mod_id") String modId,
                              String name) {
}
