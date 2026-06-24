package net.modgarden.gardenbot.client.mod_garden.event;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.Nullable;

public record EventPlatform(
		String game,
		@SerializedName("mod_loader") String modLoader,
		@SerializedName("game_version") String gameVersion
) {
	public record Modifiable(
			@Nullable String game,
			@Nullable @SerializedName("mod_loader") String modLoader,
			@Nullable @SerializedName("game_version") String gameVersion
	) {
	}
}
