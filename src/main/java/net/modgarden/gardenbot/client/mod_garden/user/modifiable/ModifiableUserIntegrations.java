package net.modgarden.gardenbot.client.mod_garden.user.modifiable;

import net.modgarden.gardenbot.client.mod_garden.user.integration.DiscordUserIntegration;
import net.modgarden.gardenbot.client.mod_garden.user.integration.MinecraftUserIntegration;
import net.modgarden.gardenbot.client.mod_garden.user.integration.ModrinthUserIntegration;
import net.modgarden.gardenbot.util.NullableWrapper;
import org.jetbrains.annotations.Nullable;

/// Defines modifiable user integrations.
/// @see NullableWrapper#of(Object)
/// @see NullableWrapper#empty()
/// @param discord 	 If wrapped, a Discord integration to set or remove if null is wrapped.
/// 			   	 If null, this field is not modified.
/// @param modrinth  If wrapped, a Modrinth integration to set or remove if null is wrapped.
/// 			   	 If null, this field is not modified.
/// @param minecraft If wrapped, a Minecraft integration to set or remove if null is wrapped.
///  			   	 If null, this field is not modified.
public record ModifiableUserIntegrations(@Nullable NullableWrapper<DiscordUserIntegration> discord,
                                         @Nullable NullableWrapper<ModrinthUserIntegration> modrinth,
                                         @Nullable NullableWrapper<MinecraftUserIntegration> minecraft) {

}
