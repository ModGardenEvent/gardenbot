package net.modgarden.gardenbot.util;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.RestAction;
import net.modgarden.gardenbot.GardenBot;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static net.modgarden.gardenbot.util.TimeUtil.*;

public class MessageCacheUtil {
	public static void cacheMessage(String userId, String messageId, String content) {
		long removalTimestamp = System.currentTimeMillis() + DAY_MS;
		try (var connection = GardenBot.createDatabaseConnection();
			 PreparedStatement statement = connection.prepareStatement("INSERT INTO message_cache (message_id, user_id, content, removal_timestamp) VALUES (?, ?, ?, ?) ON CONFLICT (message_id) DO UPDATE SET content = ?, removal_timestamp = ?")) {
			statement.setString(1, messageId);
			statement.setString(2, userId);
			statement.setString(3, content);
			statement.setLong(4, removalTimestamp);
			statement.setString(5, content);
			statement.setLong(6, removalTimestamp);
			statement.execute();
		} catch (SQLException ex) {
			GardenBot.LOG.info("Exception whilst caching message {}.", messageId, ex);
		}
	}

	@Nullable
	public static String getCachedMessage(String messageId) {
		try (var connection = GardenBot.createDatabaseConnection();
			 PreparedStatement statement = connection.prepareStatement("SELECT content FROM message_cache WHERE message_id == ?")) {
			statement.setString(1, messageId);
			return statement.executeQuery().getString(1);
		} catch (SQLException ex) {
			GardenBot.LOG.info("Exception whilst getting cached message {}.", messageId, ex);
		}
		return null;
	}

	@Nullable
	public static String getAuthorFromMessage(String messageId) {
		try (var connection = GardenBot.createDatabaseConnection();
			 PreparedStatement statement = connection.prepareStatement("SELECT user_id FROM message_cache WHERE message_id == ?")) {
			statement.setString(1, messageId);
			return statement.executeQuery().getString(1);
		} catch (SQLException ex) {
			GardenBot.LOG.info("Exception whilst getting cached message {}'s author.", messageId, ex);
		}
		return null;
	}

	public static void removeExpiredMessagesEachHour(Guild guild) {
		TimeUtil.runEachHour(() -> {
			removeExpiredMessagesFromChannel(guild.getTextChannelById(GardenBot.DOTENV.get("JOIN_LOGS_CHANNEL_ID")));
			removeExpiredMessagesFromChannel(guild.getTextChannelById(GardenBot.DOTENV.get("MODERATION_LOGS_CHANNEL_ID")));
			removeExpiredMessagesFromDatabase();
		});
	}

	private static void removeExpiredMessagesFromChannel(TextChannel channel) {
		GardenBot.LOG.info("Attempting to remove expired messages from channel...");

		Message referenceMessage = getEarliestNonExpiredMessage(channel);
		List<Message> expiredMessages = getLatestExpiredMessages(channel, referenceMessage);

		int messagesDeleted = 0;

		while (!expiredMessages.isEmpty()) {
			var messagesCopy = List.copyOf(expiredMessages).reversed();

			// Discord cannot bulk delete messages older than 2 weeks.
			// So we must bypass it for the sake of initial deletion.
			// This jank can be removed as soon as we can confirm that no message in that channel is older than 2 weeks.
			for (Message message : messagesCopy) {
				if (message.getTimeCreated().toInstant().toEpochMilli() > System.currentTimeMillis() - WEEK_MS * 2)
					break;

				RestAction<Void> deleteAction = channel.deleteMessageById(message.getId());
				// Get past rate limits by delaying the interaction by a second every 5 messages.
				if ((messagesCopy.indexOf(message) + 1) % 5 == 0) {
					deleteAction = deleteAction.delay(Duration.ofSeconds(1));
				}
				deleteAction.complete();
				expiredMessages.remove(message);
			}

			if (expiredMessages.size() == 1) {
				channel.deleteMessageById(expiredMessages.getFirst().getId()).complete();
			} else if (!expiredMessages.isEmpty()) {
				channel.deleteMessages(expiredMessages).complete();
			}
			expiredMessages = getLatestExpiredMessages(channel, referenceMessage);
			messagesDeleted += messagesCopy.size();
		}

		GardenBot.LOG.info("Successfully removed {} message(s) from channel #{}.", messagesDeleted, channel.getName());
	}

	@Nullable
	private static Message getEarliestNonExpiredMessage(TextChannel channel) {
		List<Message> allowedMessages = channel.getHistoryFromBeginning(100).complete()
				.getRetrievedHistory()
				.stream()
				.filter(message -> message.getTimeCreated().toInstant().toEpochMilli() >= System.currentTimeMillis() - DAY_MS)
				.toList();
		Message returnValue = null;
		while (!allowedMessages.isEmpty()) {
			returnValue = allowedMessages.getLast();
			allowedMessages = channel.getHistoryBefore(returnValue.getId(), 100)
					.complete()
					.getRetrievedHistory()
					.stream()
					.filter(message -> message.getTimeCreated().toInstant().toEpochMilli() >= System.currentTimeMillis() - DAY_MS)
					.toList();
		}
		return returnValue;
	}

	private static List<Message> getLatestExpiredMessages(TextChannel channel, @Nullable Message referenceMessage) {
		if (referenceMessage == null) {
			return new ArrayList<>(
					channel.getHistoryFromBeginning(100)
							.complete()
							.getRetrievedHistory()
							.stream()
							.filter(message -> message.getTimeCreated().toInstant().toEpochMilli() < System.currentTimeMillis() - DAY_MS)
							.toList()
			);
		}
		return new ArrayList<>(
				channel.getHistoryBefore(referenceMessage.getId(), 100)
						.complete()
						.getRetrievedHistory()
		);
	}

	private static void removeExpiredMessagesFromDatabase() {
		GardenBot.LOG.info("Attempting to remove expired messages from database...");
		long expiryTime = System.currentTimeMillis() - DAY_MS;

		try (var connection = GardenBot.createDatabaseConnection();
		     PreparedStatement deleteStatement = connection.prepareStatement("""
					 DELETE FROM message_cache
					 WHERE CAST(removal_timestamp AS INTEGER) < ?
					 """)
		) {
			deleteStatement.setLong(1, expiryTime);
			deleteStatement.execute();
			int updateCount = deleteStatement.getUpdateCount();
			if (updateCount == 0) {
				GardenBot.LOG.info("Successfully removed {} message(s) from database.db.", updateCount);
			} else {
				GardenBot.LOG.info("Did not remove any messages from cache.");
			}
		} catch (SQLException ex) {
			GardenBot.LOG.info("Exception whilst removing expired messages.", ex);
		}
	}
}
