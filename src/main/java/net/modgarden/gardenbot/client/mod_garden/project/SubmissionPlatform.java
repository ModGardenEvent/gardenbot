package net.modgarden.gardenbot.client.mod_garden.project;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.Nullable;

/// Represents all types of submission platform.
/// @param type The type of the platform.
/// @param projectId Used in: modrinth. The project's ID.
/// @param versionId Used in: modrinth. The version's ID.
/// @param downloadUrl Used in: download_url. The URL of the JAR to download.
public record SubmissionPlatform(String type,
                                 @Nullable @SerializedName("project_id") String projectId,
                                 @Nullable @SerializedName("version_id") String versionId,
                                 @Nullable @SerializedName("download_url") String downloadUrl) {
	public static SubmissionPlatform modrinth(String projectId, String versionId) {
		return new SubmissionPlatform("modrinth", projectId, versionId, null);
	}

	public static SubmissionPlatform downloadUrl(String downloadUrl) {
		return new SubmissionPlatform("download_url", null, null, downloadUrl);
	}
}
