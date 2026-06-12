package net.modgarden.gardenbot.modal.account;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.modgarden.gardenbot.GardenBot;
import net.modgarden.gardenbot.client.ModGarden;
import net.modgarden.gardenbot.client.exception.HypertextException;
import net.modgarden.gardenbot.client.mod_garden.user.ModGardenUser;
import net.modgarden.gardenbot.client.mod_garden.user.UserBio;
import net.modgarden.gardenbot.client.mod_garden.user.UserIntegrations;
import net.modgarden.gardenbot.client.mod_garden.user.integration.DiscordUserIntegration;
import net.modgarden.gardenbot.interaction.ModalInteraction;
import net.modgarden.gardenbot.modal.Modal;
import net.modgarden.gardenbot.response.EmbedResponse;
import net.modgarden.gardenbot.response.Response;
import org.jetbrains.annotations.NotNull;

public class RegisterModal extends Modal {
	public RegisterModal() {
		super(
				"modalRegister",
				"Register your Mod Garden account!",
				ActionRow.of(
						TextInput.create("username",
										"Username",
										TextInputStyle.SHORT
								).setRequired(false)
								.setPlaceholder("Leave this blank to use your Discord value.")
								.setMinLength(2)
								.setMaxLength(32)
								.build()
				),
				ActionRow.of(
						TextInput.create("display-name",
										"Display Name",
										TextInputStyle.SHORT
								).setRequired(false)
								.setPlaceholder("Leave this blank to use your Discord value.")
								.setMinLength(2)
								.setMaxLength(32)
								.build()
				)
		);
	}

	@NotNull
	@Override
	public Response respond(ModalInteraction interaction) {
		User discordUser = interaction.event().getUser();
		ModalMapping modalUsername = interaction.event().getValue("username");
		String username = modalUsername != null
				? modalUsername.getAsString()
				: interaction.event().getUser().getName();

		try {
			ModGardenUser mgUser = ModGarden.createUser(username);

			ModalMapping modalDisplayName = interaction.event().getValue("display-name");
			String displayName = modalDisplayName != null
					? modalDisplayName.getAsString()
					: interaction.event().getUser().getEffectiveName();

			ModGarden.modifyUserBio(mgUser,
					new UserBio(
							displayName,
							null,
							null,
							null,
							null
					)
			);

			ModGarden.modifyUserIntegrations(mgUser,
					new UserIntegrations(
							new DiscordUserIntegration(
									discordUser.getId()
							),
							null,
							null
					)
			);

			return new EmbedResponse()
					.setTitle("Your Mod Garden account has successfully been created!")
					.setColor(0xA9FFA7)
					.markEphemeral();
		} catch (HypertextException e) {
			GardenBot.LOG.error("", e);
			return new EmbedResponse()
					.setTitle("Encountered an exception whilst attempting to register your Mod Garden account.")
					.setDescription(e.getMessage() + "\nPlease report this to a team member.")
					.setColor(0xFF0000)
					.markEphemeral();
		}
	}
}
