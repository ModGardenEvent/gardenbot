package net.modgarden.gardenbot.client.mod_garden.event;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.Nullable;

public record EventTimes(
		@SerializedName("registration_open") String registrationOpen,
		@SerializedName("registration_close") String registrationClose,
		@SerializedName("development_start") String developmentStart,
		@SerializedName("development_end") String developmentEnd,
		@SerializedName("pack_freeze") String packFreeze
) {
	public record Modifiable(
			@Nullable @SerializedName("registration_open") String registrationOpen,
			@Nullable @SerializedName("registration_close") String registrationClose,
			@Nullable @SerializedName("development_start") String developmentStart,
			@Nullable @SerializedName("development_end") String developmentEnd,
			@Nullable @SerializedName("pack_freeze") String packFreeze
	) {
	}
}
