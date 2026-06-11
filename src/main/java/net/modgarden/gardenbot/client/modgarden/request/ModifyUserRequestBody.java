package net.modgarden.gardenbot.client.modgarden.request;

import net.modgarden.gardenbot.client.modgarden.user.UserBio;
import net.modgarden.gardenbot.client.modgarden.user.UserIntegrations;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record ModifyUserRequestBody(@Nullable String username,
									@Nullable UserBio bio,
									UserIntegrations integrations,
									List<String> roles) {
}
