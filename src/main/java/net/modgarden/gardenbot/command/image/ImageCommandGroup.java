package net.modgarden.gardenbot.command.image;

import net.modgarden.gardenbot.command.CommandGroup;
import net.modgarden.gardenbot.command.SlashCommand;

public class ImageCommandGroup extends CommandGroup<SlashCommand> {
	public ImageCommandGroup() {
		super(
				"image",
				"Manage images for use in showcase maps.",
				ImageUploadCommand::new
		);
	}
}
