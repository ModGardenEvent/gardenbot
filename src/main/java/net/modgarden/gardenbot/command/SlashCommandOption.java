package net.modgarden.gardenbot.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.modgarden.gardenbot.client.ModGarden;
import net.modgarden.gardenbot.client.exception.HypertextException;
import net.modgarden.gardenbot.client.mod_garden.role.ModGardenRole;
import net.modgarden.gardenbot.util.FallibleSupplier;
import net.modgarden.gardenbot.util.permission.Permission;
import org.jetbrains.annotations.NotNull;

public record SlashCommandOption(OptionType type,
                                 String name,
                                 String description,
                                 boolean required,
                                 boolean isAutoComplete) {
	public SlashCommandOption(OptionType type,
	                          String name,
	                          String description,
	                          boolean required) {
		this(type, name, description, required, false);
	}

	public static SlashCommandOption permissions(String name, boolean required) {
		return new SlashCommandOption(
				OptionType.STRING,
				name,
				"A comma separated list of permission IDs.",
				required,
				true
		);
	}

	public static SlashCommandOption role(String name, String description, boolean required) {
		return new SlashCommandOption(
				OptionType.STRING,
				name,
				description,
				required,
				true
		);
	}

	public static SlashCommandOption role(String name, boolean required) {
		return role(name, "The role.", required);
	}

	@SafeVarargs
	public static List<Command.Choice> pickChoices(FallibleSupplier<List<Command.Choice>, HypertextException>... suppliers) throws HypertextException {
		for (FallibleSupplier<List<Command.Choice>, HypertextException> supplier : suppliers) {
			List<Command.Choice> choices = supplier.get();

			if (!choices.isEmpty()) {
				return choices;
			}
		}

		return Collections.emptyList();
	}

	@NotNull
	public static List<Command.Choice> getPermissionsChoices(
			AutoCompletionGetter autoCompletionGetter,
			String optionName
	) {
		String permissions = autoCompletionGetter.getOption(optionName, OptionMapping::getAsString);

		if (permissions == null) {
			return Collections.emptyList();
		}

		List<String> splitPermissions = new ArrayList<>(List.of(permissions.replace(", ", ",").split(",")));
		String currentPermissionFriendlyName = splitPermissions.getLast();
		Permission currentPermission = Permission.fromName(currentPermissionFriendlyName);
		List<Command.Choice> choices = new ArrayList<>();
		List<String> splitPermissionNames = new ArrayList<>(splitPermissions.size());
		splitPermissions.removeLast();

		if (currentPermission != null) {
			splitPermissions.add(currentPermission.getName());
		}

		for (String name : splitPermissions) {
			splitPermissionNames.add(Permission.fromName(name).getFriendlyName());
		}

		for (Permission permission : Permission.values()) {
			if (splitPermissionNames.contains(permission.getFriendlyName())) {
				continue;
			}

			String name;
			String value;

			if (!splitPermissions.isEmpty()) {
				name = String.join(", ", splitPermissionNames) + ", " + permission.getFriendlyName();
				value = String.join(",", splitPermissions) + "," + permission.getName();
			} else {
				name = permission.getFriendlyName();
				value = permission.getName();
			}

			Command.Choice choice = new Command.Choice(name, value);
			choices.add(
					sortDifferences(permission.getFriendlyName(), currentPermissionFriendlyName, choices),
					choice
			);
		}

		removeNulls(choices);
		return choices;
	}

	@NotNull
	public static List<Command.Choice> getRoleIdChoices(
			String focusedOption,
			AutoCompletionGetter autoCompletionGetter,
			String optionName
	) throws HypertextException {
		if (focusedOption.equals(optionName)) {
			List<Command.Choice> choices = new ArrayList<>();
			String roleId = autoCompletionGetter.getOption(optionName, OptionMapping::getAsString);

			if (roleId == null) {
				return Collections.emptyList();
			}

			roleId = roleId.toLowerCase(Locale.ROOT);

			for (ModGardenRole role : ModGarden.getUserRoles()) {
				String choiceName = role.name() + " (" + role.id() + ")";
				Command.Choice choice = new Command.Choice(role.name(), role.id());
				choices.add(
						sortDifferences(role.name(), roleId, choices), choice
				);
			}

			removeNulls(choices);
			return choices;
		}

		return Collections.emptyList();
	}

	private static void removeNulls(List<Command.Choice> choices) {
		//noinspection StatementWithEmptyBody // remove until no nulls left
		while (choices.remove(null));
	}

	private static int sortDifferences(
			String first,
			String second,
			List<Command.Choice> choices
	) {
		int difference = Math.abs(first.compareToIgnoreCase(second));

		String firstL = first.toLowerCase(Locale.ROOT);
		String secondL = second.toLowerCase(Locale.ROOT);
		if (second.length() > 4 && (firstL.contains(secondL) || firstL.endsWith(secondL) || firstL.startsWith(secondL))) {
			return 0;
		}

		if (difference > 1) {
			difference += 80;
		}

		if (difference > choices.size()) {
			int i1 = difference - choices.size();
			for (int i = 0; i < i1; i++) {
				choices.add(null);
			}
		}

		return difference;
	}

	public OptionData getData() {
		OptionData data = new OptionData(type, name, description);
		data.setRequired(required);
		data.setAutoComplete(isAutoComplete);
		return data;
	}
}
