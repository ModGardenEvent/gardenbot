package net.modgarden.gardenbot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageBulkDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.modgarden.gardenbot.interaction.ButtonInteraction;
import net.modgarden.gardenbot.interaction.ModalInteraction;
import net.modgarden.gardenbot.interaction.SlashCommandInteraction;
import net.modgarden.gardenbot.interaction.button.ButtonDispatcher;
import net.modgarden.gardenbot.interaction.command.SlashCommandDispatcher;
import net.modgarden.gardenbot.interaction.modal.ModalDispatcher;
import net.modgarden.gardenbot.util.MessageCacheUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GardenBotEvents extends ListenerAdapter {
	@Override
	public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
		var response = SlashCommandDispatcher.dispatch(new SlashCommandInteraction(event));
		response.send(event).queue();
	}

	@Override
	public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {
		var choices = SlashCommandDispatcher.getAutoCompleteChoices(event);
		if (choices.isEmpty())
			return;
		event.replyChoices(choices).queue();
	}

	@Override
	public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
		var response = ButtonDispatcher.dispatch(new ButtonInteraction(event));
		response.send(event).queue();
	}

	@Override
	public void onModalInteraction(@NotNull ModalInteractionEvent event) {
		var response = ModalDispatcher.dispatch(new ModalInteraction(event));
		response.send(event).queue();
	}

	@Override
	public void onGuildReady(@NotNull GuildReadyEvent event) {
		if (!event.getGuild().getId().equals(GardenBot.DOTENV.get("GUILD_ID")))
			return;

		SlashCommandDispatcher.addCommands(event.getGuild());
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
						.setColor(0x00FF02)
						.setAuthor("User joined the server!", null, user.getEffectiveAvatarUrl())
						.setDescription(
								"Welcome <@" + user.getId() + "> (" + user.getGlobalName() +")\n" +
								"**ID:** " + user.getId())
				.build()).setAllowedMentions(List.of()).queue();
	}

	@Override
	public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent event) {
		if (GardenBot.DOTENV.get("JOIN_LOGS_CHANNEL_ID") == null || !event.getGuild().getId().equals(GardenBot.DOTENV.get("GUILD_ID")))
			return;

		String joinLogsChannelId = GardenBot.DOTENV.get("JOIN_LOGS_CHANNEL_ID");
		TextChannel channel = event.getGuild().getTextChannelById(joinLogsChannelId);

		if (channel == null)
			return;

		User user = event.getUser();
		channel.sendMessageEmbeds(new EmbedBuilder()
				.setColor(0x00FF02)
				.setAuthor("User left the server!", null, user.getEffectiveAvatarUrl())
				.setDescription("Goodbye <@" + user.getId() + "> (" + user.getGlobalName() +")\n" +
						"**ID:** " + user.getId())
				.build()).setAllowedMentions(List.of()).queue();
	}

	@Override
	public void onMessageReceived(@NotNull MessageReceivedEvent event) {
		if (GardenBot.DOTENV.get("MODERATION_LOGS_CHANNEL_ID") == null || !event.getGuild().getId().equals(GardenBot.DOTENV.get("GUILD_ID")) || event.getAuthor().isSystem() || event.getAuthor().isBot() || event.getMessage().getContentRaw().isEmpty() || event.isWebhookMessage())
			return;

		MessageCacheUtil.cacheMessage(event.getAuthor().getId(), event.getMessageId(), event.getMessage().getContentDisplay());
	}

	@Override
	public void onMessageUpdate(@NotNull MessageUpdateEvent event) {
		if (GardenBot.DOTENV.get("MODERATION_LOGS_CHANNEL_ID") == null || !event.getGuild().getId().equals(GardenBot.DOTENV.get("GUILD_ID")) || event.getAuthor().isSystem() || event.getAuthor().isBot() || event.getMessage().getContentRaw().isEmpty() || event.getMessage().isWebhookMessage())
			return;

		String moderationLogsChannelId = GardenBot.DOTENV.get("MODERATION_LOGS_CHANNEL_ID");
		TextChannel channel = event.getGuild().getTextChannelById(moderationLogsChannelId);

		if (channel == null)
			return;

		String description = "**Channel:** <#" + event.getMessage().getChannelId() + ">\n" +
				"**Author:** <@" + event.getAuthor().getId() + "> (" + event.getAuthor().getName() + ")\n" +
				"**Author ID:** " + event.getAuthor().getId();

		String oldMessageContent = MessageCacheUtil.getCachedMessage(event.getMessageId());
		if (oldMessageContent == null)
			return;

		channel.sendMessageEmbeds(new EmbedBuilder()
				.setColor(0x00FF02)
				.setAuthor("Message Edited!", null, event.getAuthor().getEffectiveAvatarUrl())
				.setDescription(description)
				.addField("Old Message", oldMessageContent, false)
				.addField("New Message", event.getMessage().getContentDisplay(), false)
				.build()).setAllowedMentions(List.of()).queue();

		MessageCacheUtil.cacheMessage(event.getAuthor().getId(), event.getMessageId(), event.getMessage().getContentDisplay());
	}

	@Override
	public void onMessageDelete(@NotNull MessageDeleteEvent event) {
		if (GardenBot.DOTENV.get("MODERATION_LOGS_CHANNEL_ID") == null || !event.getGuild().getId().equals(GardenBot.DOTENV.get("GUILD_ID")))
			return;

		String moderationLogsChannelId = GardenBot.DOTENV.get("MODERATION_LOGS_CHANNEL_ID");
		TextChannel channel = event.getGuild().getTextChannelById(moderationLogsChannelId);

		if (channel == null)
			return;

		String oldMessageContent = MessageCacheUtil.getCachedMessage(event.getMessageId());

		if (oldMessageContent == null)
			return;

		String userId = MessageCacheUtil.getAuthorFromMessage(event.getMessageId());
		Member member = null;

		String description = "**Channel:** <#" + event.getGuildChannel().getId() + ">";
		if (userId != null) {
			member = event.getGuild().getMemberById(userId);
			if (member != null && (member.getUser().isBot() || member.getUser().isSystem()))
				return;
			String globalName = member != null && member.getUser().getGlobalName() != null ? member.getUser().getName() : "unknown";
				description = description + "\n" +
						"**Author:** <@" + userId + "> (" + globalName + ")\n" +
						"**Author ID:** " + userId;
		}

		var builder = new EmbedBuilder()
				.setColor(0xA60002)
				.setDescription(description)
				.addField("Message", oldMessageContent, false);

		if (member != null)
			builder.setAuthor("Message Deleted!", null, member.getEffectiveAvatarUrl());
		else
			builder.setAuthor("Message Deleted!");

		channel.sendMessageEmbeds(builder.build()).setAllowedMentions(List.of()).queue();

		MessageCacheUtil.deleteCachedMessage(event.getMessageId());
	}

	@Override
	public void onMessageBulkDelete(MessageBulkDeleteEvent event) {
		for (String messageId : event.getMessageIds())
			MessageCacheUtil.deleteCachedMessage(messageId);
	}
}
