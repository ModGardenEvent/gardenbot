package net.modgarden.gardenbot.client.mod_garden.event;

import com.google.gson.annotations.SerializedName;

public record EventPlatform(String game,
                            @SerializedName("mod_loader") String modLoader,
                            @SerializedName("game_version") String gameVersion) {
}
