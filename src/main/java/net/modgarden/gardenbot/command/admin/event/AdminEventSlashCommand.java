package net.modgarden.gardenbot.command.admin.event;

import net.modgarden.gardenbot.command.SlashCommandOption;
import net.modgarden.gardenbot.command.admin.AdminSlashCommand;
import net.modgarden.gardenbot.util.permission.Permission;
import net.modgarden.gardenbot.util.permission.PermissionPredicate;
import org.jetbrains.annotations.Nullable;

public abstract class AdminEventSlashCommand extends AdminSlashCommand {
	public AdminEventSlashCommand(
			String name,
			String description,
			SlashCommandOption... options
	) {
		super(name, description, options);
	}

	@Nullable
	@Override
	protected PermissionPredicate requiredPermissions() {
		return PermissionPredicate.all(Permission.EDIT_EVENT);
	}
}
