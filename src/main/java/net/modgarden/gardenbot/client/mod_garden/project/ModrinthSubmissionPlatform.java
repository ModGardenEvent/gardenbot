package net.modgarden.gardenbot.client.mod_garden.project;

import com.google.gson.annotations.SerializedName;

public record ModrinthSubmissionPlatform(String type,
                                         @SerializedName("project_id") String projectId,
                                         @SerializedName("version_id") String versionId) {
	public ModrinthSubmissionPlatform(String projectId, String versionId) {
		this("modrinth", projectId, versionId);
	}
}
