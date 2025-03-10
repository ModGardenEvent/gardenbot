package net.modgarden.gardenbot.interaction.modal;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.modgarden.gardenbot.commands.account.RegisterCommandHandler;

public class RegisterModal extends SimpleModal {
	public RegisterModal() {
		super("modalRegister", "Register your Mod Garden account!", RegisterCommandHandler::handleModal);
	}

	@Override
	public Modal getModal(SlashCommandInteractionEvent interaction) {
		User user = interaction.getUser();
		return Modal.create(ID, TITLE)
				.addComponents(
						ActionRow.of(
								TextInput.create("username",
												"Username",
												TextInputStyle.SHORT
										).setRequired(true)
										.setValue(user.getName())
										.setMaxLength(48).build()
						),
						ActionRow.of(
								TextInput.create("displayName",
												"Display Name",
												TextInputStyle.SHORT
										).setRequired(true)
										.setValue(user.getGlobalName())
										.setMaxLength(48).build()
						)
				).build();
	}
}
