package net.modgarden.gardenbot.database.fixer;

import net.modgarden.gardenbot.GardenBot;
import net.modgarden.gardenbot.database.fixer.fix.V1ToV2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class DatabaseFixer {
	private static final List<DatabaseFix> FIXES = new ArrayList<>();

	public static void createFixers() {
		Collections.addAll(
				FIXES,
				new V1ToV2()
		);
	}

	public static int getSchemaVersion() {
		if (FIXES.isEmpty()) {
			return -1;
		}
		return FIXES.getLast().getVersionToFixFrom() + 1;
	}

	public static void fixDatabase() {
		long startTime = System.currentTimeMillis();
		int version = -1;
		try (Connection connection = GardenBot.createDatabaseConnection();
		     PreparedStatement schemaVersion = connection.prepareStatement("SELECT version FROM schema")) {
			ResultSet query = schemaVersion.executeQuery();

			version = query.getInt(1);
			int lastVersion = getSchemaVersion();
			if (lastVersion == -1 || version > lastVersion) {
				throw new IllegalStateException("Schema version is invalid! Got " + lastVersion + ", " + version + " in the database");
			}
			if (version == lastVersion)
				return;

		} catch (Exception ex) {
			GardenBot.LOG.error("Failed to fix data: ", ex);
		}

		for (DatabaseFix fix : FIXES) {
			Consumer<Connection> dropper = null;
			try (Connection connection = GardenBot.createDatabaseConnection()) {
				dropper = fix.fixInternal(connection, version);
			} catch (SQLException ex) {
				GardenBot.LOG.error("Failed to fix data: ", ex);
			}

			try (Connection connection = GardenBot.createDatabaseConnection()) {
				if (dropper != null) {
					dropper.accept(connection);
				}
			} catch (SQLException ex) {
				GardenBot.LOG.error("Failed to fix data: ", ex);
			}
		}
		long endTime = System.currentTimeMillis();
		GardenBot.LOG.info("Data-fixer took {}ms", endTime - startTime);
	}
}
