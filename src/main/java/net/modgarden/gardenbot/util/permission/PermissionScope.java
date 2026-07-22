package net.modgarden.gardenbot.util.permission;

import java.util.Locale;
import java.util.Objects;

public enum PermissionScope {
	ALL,
	USER,
	PROJECT,;

	public static PermissionScope fromString(String string) {
		return valueOf(string.toUpperCase(Locale.ROOT));
	}

	public boolean matches(PermissionScope scope) {
		if (this.equals(ALL) || Objects.equals(scope, ALL)) {
			return true;
		}

		return this.equals(scope);
	}
}
