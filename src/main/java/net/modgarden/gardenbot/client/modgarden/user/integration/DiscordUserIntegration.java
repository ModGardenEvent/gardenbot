package net.modgarden.gardenbot.client.modgarden.user.integration;

import com.google.gson.annotations.SerializedName;

public record DiscordUserIntegration(@SerializedName("user_id") String userId) {
}
