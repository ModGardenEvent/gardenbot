package net.modgarden.gardenbot.command.fix;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.modgarden.gardenbot.GardenBot;
import net.modgarden.gardenbot.client.Discord;
import net.modgarden.gardenbot.client.ModGarden;
import net.modgarden.gardenbot.client.exception.HypertextException;
import net.modgarden.gardenbot.command.SlashCommand;
import net.modgarden.gardenbot.interaction.SlashCommandInteraction;
import net.modgarden.gardenbot.response.MessageResponse;
import net.modgarden.gardenbot.response.Response;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class FixRolesCommand extends SlashCommand {
	public FixRolesCommand() {
		super(
				"roles",
				"Fixes your roles within Mod Garden and Discord's data."
		);
	}

	@NotNull
	@Override
	public Response respond(SlashCommandInteraction interaction) {
		if (interaction.event().getGuild() == null || !interaction.event().getGuild().getId().equals(GardenBot.DOTENV.get("GUILD_ID")))
			return new MessageResponse("You must run this command within the Mod Garden Discord server!");

		Member member = interaction.event().getMember();
		assert member != null;

		try {
			List<Role> addedRoles = Discord.addModGardenRolesToDiscordUser(interaction.event().getGuild(), member);

			List<Role> discordRoles = new ArrayList<>(member.getRoles());
			discordRoles.removeAll(addedRoles);
			ModGarden.addDiscordRolesToModGardenUser(member, discordRoles);

			return new MessageResponse("Fixed your roles within the Discord and the Mod Garden Backend!");
		} catch (HypertextException e) {
			return exceptionResponse(e);
		}
	}
}
