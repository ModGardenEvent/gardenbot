package net.modgarden.gardenbot.client.mod_garden.role.integration;

import com.google.gson.annotations.SerializedName;

public record DiscordRoleIntegration(@SerializedName("role_id") String roleId) {
}
