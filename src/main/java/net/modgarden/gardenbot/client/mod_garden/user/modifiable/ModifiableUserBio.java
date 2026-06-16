package net.modgarden.gardenbot.client.mod_garden.user.modifiable;

import com.google.gson.annotations.SerializedName;
import net.modgarden.gardenbot.util.NullableWrapper;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/// Defines a modifiable user bio.
/// @see NullableWrapper#of(Object)
/// @see NullableWrapper#empty()
/// @param displayName If wrapped, a display name to set or remove if null is wrapped.
/// 			   	   If null, this field is not modified.
/// @param modrinth  If wrapped, a Modrinth integration to set or remove.
/// 			   	 If null, this field is not modified.
/// @param minecraft If wrapped, a Minecraft integration to set or remove.
///  			   	 If null, this field is not modified.
public record ModifiableUserBio(@SerializedName("display_name") @Nullable NullableWrapper<String> displayName,
                                @Nullable NullableWrapper<String> pronouns,
                                @Nullable NullableWrapper<String> description,
                                @Nullable Map<String, NullableWrapper<String>> fields) {
}
