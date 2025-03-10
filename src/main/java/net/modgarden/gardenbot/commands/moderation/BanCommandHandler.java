package net.modgarden.gardenbot.commands.moderation;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.modgarden.gardenbot.interaction.response.EmbedResponse;
import net.modgarden.gardenbot.interaction.SlashCommandInteraction;
import net.modgarden.gardenbot.interaction.response.Response;

public class BanCommandHandler {
	public static Response handleBan(SlashCommandInteraction interaction) {
		EmbedResponse response = new EmbedResponse();

		Member member = interaction.event().getMember();
		if (member == null) {
			response.setTitle("Error");
			response.setDescription("Cannot ban a user without a member behind the ban.");
			response.markEphemeral();
			return response;
		}

		if (!member.hasPermission(Permission.BAN_MEMBERS)) {
			response.setTitle("Error");
			response.setDescription("You do not have the permissions to ban users.");
			response.markEphemeral();
			return response;
		}

		// TODO: Do this later.
		return response.setTitle("Successfully banned user!").setDescription("Testing successful!").markEphemeral();
	}
}
