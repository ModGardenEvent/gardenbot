package net.modgarden.gardenbot.client.mod_garden.project;

public record ModrinthSubmissionPlatform(String type,
                                         String projectId,
                                         String versionId) {
	public ModrinthSubmissionPlatform(String projectId, String versionId) {
		this("modrinth", projectId, versionId);
	}
}
