package net.modgarden.gardenbot;

import ch.qos.logback.classic.Level;
import com.google.gson.Gson;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.modgarden.gardenbot.util.MessageCacheUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.http.HttpClient;
import java.sql.*;

public class GardenBot {
	public static final Logger LOG = LoggerFactory.getLogger("GardenBot");
	public static final String API_URL = "development".equals(System.getenv("env")) ? "http://localhost:7070/v1/" : "https://api.modgarden.net/v1/";
	public static final String VERSION = "1.1.1";
	public static final Gson GSON = new Gson();

	public static final Dotenv DOTENV = Dotenv.load();
	public static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

	private static final int DATABASE_SCHEMA_VERSION = 4;

	public static JDA jda;

	public static void main(String[] args) {
		if ("development".equals(System.getenv("env")))
			((ch.qos.logback.classic.Logger)LOG).setLevel(Level.DEBUG);

		jda = JDABuilder.create(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
				.disableCache(CacheFlag.ACTIVITY, CacheFlag.VOICE_STATE, CacheFlag.EMOJI, CacheFlag.STICKER, CacheFlag.CLIENT_STATUS, CacheFlag.ONLINE_STATUS, CacheFlag.SCHEDULED_EVENTS)
				.setToken(DOTENV.get("TOKEN"))
				.addEventListeners(new GardenBotEvents())
				.build();

		try {
			if (new File("./database.db").createNewFile()) {
				LOG.info("Successfully created database file.");
			}
			createDatabaseContents();
			updateSchemaVersion();
		} catch (IOException ex) {
			LOG.error("Failed to create database file.", ex);
		}

		GardenBotCommands.registerAll();
		GardenBotButtonHandlers.registerAll();
		MessageCacheUtil.removeExpiredMessagesStartOfDay();

		LOG.info("GardenBot has been initialized.");
    }

	public static Connection createDatabaseConnection() throws SQLException {
		String url = "jdbc:sqlite:database.db";
		return DriverManager.getConnection(url);
	}

	private static void createDatabaseContents() {
		try (Connection connection = createDatabaseConnection();
			 Statement statement = connection.createStatement()) {
			statement.addBatch("CREATE TABLE IF NOT EXISTS bans (" +
					"id TEXT UNIQUE NOT NULL," +
					"username TEXT NOT NULL," +
					"unban_time TEXT NOT NULL," +
					"reason TEXT NOT NULL," +
					"PRIMARY KEY(id)" +
					")");
			statement.addBatch("CREATE TABLE IF NOT EXISTS message_cache (" +
					"message_id TEXT UNIQUE NOT NULL," +
					"user_id TEXT NOT NULL," +
					"content TEXT NOT NULL," +
					"removal_timestamp TEXT NOT NULL," +
					"PRIMARY KEY(message_id)" +
					")");
			statement.executeBatch();
		} catch (SQLException ex) {
			LOG.error("Failed to create database tables. ", ex);
			return;
		}
		LOG.debug("Created database tables.");
	}

	private static void updateSchemaVersion() {
		try (Connection connection = createDatabaseConnection();
			 Statement statement = connection.createStatement()) {
			statement.addBatch("CREATE TABLE IF NOT EXISTS schema (version INTEGER NOT NULL, PRIMARY KEY(version))");
			statement.addBatch("DELETE FROM schema");
			statement.executeBatch();
			try (PreparedStatement prepared = connection.prepareStatement("INSERT INTO schema VALUES (?)")) {
				prepared.setInt(1, DATABASE_SCHEMA_VERSION);
				prepared.execute();
			}
		} catch (SQLException ex) {
			LOG.error("Failed to update database schema version. ", ex);
			return;
		}
		LOG.debug("Updated database schema version.");
	}
}
