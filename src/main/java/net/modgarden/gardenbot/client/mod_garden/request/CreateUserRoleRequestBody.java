package net.modgarden.gardenbot.client.mod_garden.request;

import net.modgarden.gardenbot.client.mod_garden.role.RoleIntegrations;

public record CreateUserRoleRequestBody(String name,
                                        String permissions,
                                        RoleIntegrations integrations) {
}
