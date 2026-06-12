package net.modgarden.gardenbot.client.mod_garden.request;

import net.modgarden.gardenbot.client.mod_garden.user.UserBio;
import net.modgarden.gardenbot.client.mod_garden.user.UserIntegrations;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record ModifyUserRequestBody(@Nullable String username,
                                    @Nullable UserBio bio,
                                    UserIntegrations integrations,
                                    List<String> roles) {
}
