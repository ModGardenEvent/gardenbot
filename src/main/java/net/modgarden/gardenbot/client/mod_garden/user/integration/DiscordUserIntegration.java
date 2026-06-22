package net.modgarden.gardenbot.client.mod_garden.user.integration;

import com.google.gson.annotations.SerializedName;

public record DiscordUserIntegration(@SerializedName("user_id") String userId) {
}
