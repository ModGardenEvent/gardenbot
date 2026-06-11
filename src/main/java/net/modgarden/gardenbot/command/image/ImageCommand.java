package net.modgarden.gardenbot.command.image;

import net.modgarden.gardenbot.command.GroupSlashCommand;
import net.modgarden.gardenbot.command.SlashCommand;

public class ImageCommand extends GroupSlashCommand<SlashCommand> {
	public ImageCommand() {
		super(
				"image",
				"Manage images for use in showcase maps.",
				ImageUploadCommand::new
		);
	}
}
