package net.modgarden.gardenbot.client.modgarden.user;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public record UserBio(@SerializedName("display_name") @Nullable String displayName,
                      @Nullable String pronouns,
                      @Nullable String description,
                      @SerializedName("avatar_url")  @Nullable String avatarUrl,
                      @Nullable Map<String, String> fields) {
}
