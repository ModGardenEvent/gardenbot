package net.modgarden.gardenbot.client.mod_garden.request;

import net.modgarden.gardenbot.client.mod_garden.user.modifiable.ModifiableUserBio;
import net.modgarden.gardenbot.client.mod_garden.user.modifiable.ModifiableUserIntegrations;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record ModifyUserRequestBody(@Nullable String username,
                                    @Nullable ModifiableUserBio bio,
                                    @Nullable ModifiableUserIntegrations integrations,
                                    List<String> roles) {
}
