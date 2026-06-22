package net.modgarden.gardenbot.client.mod_garden.event;

import com.google.gson.annotations.SerializedName;

public record EventTimes(@SerializedName("registration_open") String registrationOpen,
                         @SerializedName("registration_close") String registrationClose,
                         @SerializedName("development_start") String developmentStart,
                         @SerializedName("development_end") String developmentEnd,
                         @SerializedName("pack_freeze") String packFreeze) {
}
