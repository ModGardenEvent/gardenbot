package net.modgarden.gardenbot.client.modgarden.event;

import com.google.gson.annotations.SerializedName;

public record EventTimes(@SerializedName("registration_open") String registrationOpen,
                         @SerializedName("registration_close") String registrationClose,
						 @SerializedName("pack_freeze") String packFreeze) {
}
