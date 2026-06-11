package net.modgarden.gardenbot.client.modgarden.role.integration;

import com.google.gson.annotations.SerializedName;

public record DiscordRoleIntegration(@SerializedName("role_id") String roleId) {
}
