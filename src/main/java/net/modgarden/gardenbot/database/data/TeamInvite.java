package net.modgarden.gardenbot.database.data;

public record TeamInvite(String id,
						 String userId,
						 String projectId,
						 String projectName,
						 String role,
						 long expirationTime) {
}
