package net.modgarden.gardenbot.util;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.modgarden.gardenbot.GardenBot;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MessageCacheUtil {
	public static void cacheMessage(String userId, String messageId, String content) {
		long removalTimestamp = System.currentTimeMillis() + TimeUtil.DAY;
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
		long currentTime = System.currentTimeMillis() + TimeUtil.DAY;
		TextChannel channel = jda.getTextChannelById(GardenBot.DOTENV.get("MODERATION_LOGS_CHANNEL_ID"));

		try (var connection = GardenBot.createDatabaseConnection();
			 PreparedStatement selectStatement = connection.prepareStatement("""
				SELECT message_id
				FROM message_cache
				WHERE CAST(removal_timestamp AS INTEGER) >= ?
				ORDER BY (removal_timestamp)
				LIMIT = 1
				""");
			 PreparedStatement deleteStatement = connection.prepareStatement("""
				DELETE FROM message_cache
				WHERE CAST(removal_timestamp AS INTEGER) < ?
				""")
		) {
			if (channel != null) {
				selectStatement.setLong(1, currentTime);
				ResultSet oldestValidMessageQuery = selectStatement.executeQuery();
				String referenceMessage = !oldestValidMessageQuery.isBeforeFirst()
						? null
						: oldestValidMessageQuery.getString("message_id");
				MessageHistory history = getMessagesBeforeCurrent(channel, referenceMessage);
				while (!history.isEmpty()) {
					channel.deleteMessages(history.getRetrievedHistory()).complete();
					history = getMessagesBeforeCurrent(channel, referenceMessage);
				}
			}

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

	private static MessageHistory getMessagesBeforeCurrent(TextChannel channel, @Nullable String referenceMessage) {
		if (referenceMessage == null) {
			return channel.getHistoryFromBeginning(100).complete();
		} else {
			return channel.getHistoryBefore(referenceMessage, 100).complete();
		}
	}
}
