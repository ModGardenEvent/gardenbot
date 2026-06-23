package net.modgarden.gardenbot.util.permission;

import static net.modgarden.gardenbot.util.permission.PermissionScope.ALL;
import static net.modgarden.gardenbot.util.permission.PermissionScope.USER;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

public enum Permission {
	/// Signifies that this user has every permission.
	/// Do not give this out unless it is absolutely necessary for an individual team member to receive this.
	ADMINISTRATOR(0x1, "administrator", ALL, "Administrator"),
	/// Create, edit, and manage roles for users or projects.
	MANAGE_ROLES(0x2, "manage_roles", ALL, "Manage Roles"),
	/// Create, edit, and hide events.
	EDIT_EVENT(0x4, "edit_event", USER, "Edit Event"),
	/// Edit others' profiles and punish users.
	MODERATE_USERS(0x8, "moderate_users", USER, "Moderate Users"),
	/// Edit your own profile.
	EDIT_PROFILE(0x10, "edit_profile", USER, "Edit Profile"),
	/// Edit this project.
	EDIT_PROJECT(0x20, "edit_project", ALL, "Edit Project"),
	/// Join, participate, and submit projects in events.
	PARTICIPATE(0x40, "participate", USER, "Participate"),
	/// Upload files to the CDN.
	UPLOAD_TO_CDN(0x80, "upload_to_cdn", USER, "Upload To CDN"),
	/// List, modify, and delete files in the CDN.
	MANAGE_CDN(0x100, "manage_cdn", USER, "Manage CDN"),
	/// Generate and delete API keys on behalf of this user or project.
	MODIFY_API_KEY(0x200, "modify_api_key", ALL, "Modify API Key"),
	/// List information about users, user roles, or any other user-related data.
	///
	/// This permission exists to prevent user enumeration, primarily
	/// to prevent [brute force attacks](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html#protect-against-automated-attacks).
	/// See [OWASP's Authentication Cheatsheet](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html#authentication-responses)
	/// for more information.
	LIST_USER_INFO(0x400, "list_user_info", USER, "List User Info");

	private static final Set<String> names = new HashSet<>();
	private static final HashMap<String, Permission> NAME_2_PERMISSION = new HashMap<>();

	private final long bit;
	private final String name;
	private final PermissionScope kind;
	private final String friendlyName;

	Permission(int bit, String name, PermissionScope kind,
			String friendlyName
	) {
		this.bit = bit;
		this.name = name;
		this.kind = kind;
		this.friendlyName = friendlyName;
	}

	public static Set<String> names() {
		if (names.isEmpty()) {
			for (Permission permission : Permission.values()) {
				names.add(permission.getName());
			}
		}

		return names;
	}

	public static Permission fromName(String name) {
		if (NAME_2_PERMISSION.isEmpty()) {
			for (Permission permission : Permission.values()) {
				NAME_2_PERMISSION.put(permission.getName(), permission);
				NAME_2_PERMISSION.put(permission.getFriendlyName(), permission);
			}
		}

		return NAME_2_PERMISSION.get(name);
	}

	public static List<Permission> fromLong(long value, PermissionScope kind) {
		List<Permission> permissions = new ArrayList<>();
		for (Permission permission : Permission.values(kind)) {
			if (hasPermissionRaw(value, permission)) {
				permissions.add(permission);
			}
		}
		return permissions;
	}

	public static List<Permission> fromLongString(String value, PermissionScope kind) {
		return fromLong(Long.parseLong(value), kind);
	}

	public static long toLong(List<Permission> permissions) {
		long value = 0;
		for (Permission permission : permissions) {
			value = grantPermission(value, permission);
		}
		return value;
	}

	public static String toLongString(List<Permission> permissions) {
		return Long.toString(toLong(permissions));
	}

	private static long grantPermission(long previousValue, Permission permission) {
		long newValue = previousValue;
		newValue |= permission.bit;
		return newValue;
	}

	private static boolean hasPermissionRaw(long userPermissions, Permission permission) {
		return (userPermissions & permission.bit) != 0;
	}

	private static List<Permission> values(PermissionScope kind) {
		List<Permission> permissions = new ArrayList<>();
		for (Permission permission : Permission.values()) {
			if (permission.kind == ALL || permission.kind == kind) {
				permissions.add(permission);
			}
		}
		return permissions;
	}

	public long getBit() {
		return this.bit;
	}

	public String getName() {
		return name;
	}

	public String getFriendlyName() {
		return friendlyName;
	}
}
