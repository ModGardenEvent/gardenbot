package net.modgarden.gardenbot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.modgarden.gardenbot.interaction.ModalInteraction;
import net.modgarden.gardenbot.interaction.SlashCommandInteraction;
import net.modgarden.gardenbot.interaction.command.SlashCommandDispatcher;
import net.modgarden.gardenbot.interaction.modal.ModalDispatcher;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GardenBotEvents extends ListenerAdapter {
	@Override
	public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
		var response = SlashCommandDispatcher.dispatch(new SlashCommandInteraction(event));
		response.send(event).queue();
	}

	@Override
	public void onModalInteraction(@NotNull ModalInteractionEvent event) {
		var response = ModalDispatcher.dispatch(new ModalInteraction(event));
		response.send(event).queue();
	}

	@Override
	public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
		if (GardenBot.DOTENV.get("JOIN_LOGS_CHANNEL_ID") == null || !event.getGuild().getId().equals(GardenBot.DOTENV.get("GUILD_ID")))
			return;

		String guildChannelId = GardenBot.DOTENV.get("JOIN_LOGS_CHANNEL_ID");

		TextChannel channel = event.getGuild().getTextChannelById(guildChannelId);

		if (channel == null)
			return;

		User user = event.getUser();
		channel.sendMessageEmbeds(new EmbedBuilder()
						.setColor(0x46883B)
						.setAuthor("User joined the server!", user.getAvatarUrl())
						.setDescription(
								"Welcome <@" + user.getId() + "> (" + user.getGlobalName() +")\n" +
								"**ID:** " + user.getId())
				.build()).setAllowedMentions(List.of()).queue();
	}

	@Override
	public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent event) {
		if (GardenBot.DOTENV.get("JOIN_LOGS_CHANNEL_ID") == null || !event.getGuild().getId().equals(GardenBot.DOTENV.get("GUILD_ID")))
			return;

		String guildChannelId = GardenBot.DOTENV.get("JOIN_LOGS_CHANNEL_ID");

		TextChannel channel = event.getGuild().getTextChannelById(guildChannelId);

		if (channel == null)
			return;

		User user = event.getUser();
		channel.sendMessageEmbeds(new EmbedBuilder()
				.setColor(0x46883B)
				.setAuthor("User left the server!", user.getAvatarUrl())
				.setDescription("Goodbye <@" + user.getId() + "> (" + user.getGlobalName() +")\n" +
						"**ID:** " + user.getId())
				.build()).setAllowedMentions(List.of()).queue();
	}
}
