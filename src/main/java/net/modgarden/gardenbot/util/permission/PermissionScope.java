package net.modgarden.gardenbot.util.permission;

import java.util.Locale;

public enum PermissionScope {
	ALL,
	USER,
	PROJECT,;

	public static PermissionScope fromString(String string) {
		return valueOf(string.toUpperCase(Locale.ROOT));
	}
}
