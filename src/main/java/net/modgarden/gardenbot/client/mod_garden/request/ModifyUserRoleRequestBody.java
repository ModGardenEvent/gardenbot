package net.modgarden.gardenbot.client.mod_garden.request;

import org.jetbrains.annotations.Nullable;

public record ModifyUserRoleRequestBody(@Nullable String name,
                                        @Nullable String permissions) {
}
