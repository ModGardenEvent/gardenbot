package net.modgarden.gardenbot;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.modgarden.gardenbot.util.MessageCacheUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.http.HttpClient;
import java.sql.*;
import java.util.EnumSet;

public class GardenBot {
	public static final Logger LOG = LoggerFactory.getLogger("GardenBot");
	public static final String API_URL = "development".equals(System.getenv("env")) ? "http://localhost:7070" : "https://api.modgarden.net";

	public static final Dotenv DOTENV = Dotenv.load();
	public static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

	public static final String SAFE_URL_REGEX = "[a-zA-Z0-9!@$()`.+,_\"-]+";
	private static final int DATABASE_SCHEMA_VERSION = 1;

	public static JDA jda;

	public static void main(String[] args) {
		jda = JDABuilder.create(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
				.setToken(DOTENV.get("TOKEN"))
				.addEventListeners(new GardenBotEvents())
				.build();

		try {
			if (new File("./database.db").createNewFile()) {
				LOG.info("Successfuly created database file.");
			}
			createDatabaseContents();
			updateSchemaVersion();
		} catch (IOException ex) {
			LOG.error("Failed to create database file.", ex);
		}

		GardenBotCommands.registerAll();
		MessageCacheUtil.removeExpiredMessagesStartOfDay();
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
		LOG.info("Created database tables.");
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
		LOG.info("Updated database schema version.");
	}
}
