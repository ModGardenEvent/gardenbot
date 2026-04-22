package net.modgarden.gardenbot.util;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.modgarden.gardenbot.GardenBot;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import static net.modgarden.gardenbot.util.TimeUtil.DAY_MS;

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

	public static void removeExpiredMessagesEachHour(JDA jda) {
		TimeUtil.runEachHour(() -> removeExpiredMessages(jda));
	}

	private static void removeExpiredMessages(JDA jda) {
		GardenBot.LOG.info("Attempting to remove expired messages...");
		long currentTime = System.currentTimeMillis() + DAY_MS;
		TextChannel channel = jda.getTextChannelById(GardenBot.DOTENV.get("MODERATION_LOGS_CHANNEL_ID"));

		if (channel != null) {
			Message referenceMessage = getEarliestNonExpiredMessages(channel);
			List<Message> expiredMessages = getLatestExpiredMessages(channel, referenceMessage);
			while (!expiredMessages.isEmpty()) {
				channel.deleteMessages(expiredMessages).complete();
				expiredMessages = getLatestExpiredMessages(channel, referenceMessage);
			}
		}

		try (var connection = GardenBot.createDatabaseConnection();
			 PreparedStatement deleteStatement = connection.prepareStatement("""
				DELETE FROM message_cache
				WHERE CAST(removal_timestamp AS INTEGER) < ?
				""")
		) {
			deleteStatement.setLong(1, currentTime);
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

	@Nullable
	private static Message getEarliestNonExpiredMessages(TextChannel channel) {
		List<Message> allowedMessages = channel.getHistoryFromBeginning(100).complete()
				.getRetrievedHistory()
				.stream()
				.filter(message -> message.getTimeCreated().toInstant().toEpochMilli() >= System.currentTimeMillis())
				.toList();
		Message returnValue = null;
		while (!allowedMessages.isEmpty()) {
			returnValue = allowedMessages.getLast();
		}
		return returnValue;
	}

	private static List<Message> getLatestExpiredMessages(TextChannel channel, @Nullable Message referenceMessage) {
		if (referenceMessage == null) {
			return channel.getHistoryFromBeginning(100).complete().getRetrievedHistory();
		}
		return channel.getHistoryBefore(referenceMessage.getId(), 100).complete().getRetrievedHistory();
	}
}
