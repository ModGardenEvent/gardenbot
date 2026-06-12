package net.modgarden.gardenbot.client.mod_garden.event;

import com.google.gson.annotations.SerializedName;

// TODO: Idk if we end up running an event for another game... Probably won't happen... Too bad!
public record EventPlatform(String game,
                            @SerializedName("mod_loader") String modLoader,
                            @SerializedName("game_version") String gameVersion) {
}
