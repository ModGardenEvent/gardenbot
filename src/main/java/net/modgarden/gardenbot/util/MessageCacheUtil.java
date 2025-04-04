package net.modgarden.gardenbot.util;

import net.modgarden.gardenbot.GardenBot;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class MessageCacheUtil {
	public static void cacheMessage(String userId, String messageId, String content) {
		long removalTimestamp = TimeUtil.closestStartOfDay(System.currentTimeMillis()) + 86400000L * 30;
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

	public static void deleteCachedMessage(String messageId) {
		try (var connection = GardenBot.createDatabaseConnection();
			 PreparedStatement statement = connection.prepareStatement("DELETE FROM message_cache WHERE message_id == ?")) {
			statement.setString(1, messageId);
			statement.execute();
		} catch (SQLException ex) {
			GardenBot.LOG.info("Exception whilst deleting cached message {}.", messageId, ex);
		}
	}

	public static void removeExpiredMessagesStartOfDay() {
		TimeUtil.runEachStartOfDay(MessageCacheUtil::removeExpiredMessages);
	}

	private static void removeExpiredMessages() {
		GardenBot.LOG.info("Attempting to remove expired messages...");
		long currentTime = TimeUtil.closestStartOfDay(System.currentTimeMillis());

		try (var connection = GardenBot.createDatabaseConnection();
			 PreparedStatement statement = connection.prepareStatement("DELETE FROM message_cache WHERE CAST(removal_timestamp AS INTEGER) <= ?")) {
			statement.setLong(1, currentTime);
			statement.execute();
			int updateCount = statement.getUpdateCount();
			if (updateCount == 0)
				GardenBot.LOG.info("Successfully removed {} message(s) from database.db.", updateCount);
			else
				GardenBot.LOG.info("Did not remove any messages from cache.");
		} catch (SQLException ex) {
			GardenBot.LOG.info("Exception whilst removing expired messages.", ex);
		}
	}
}
