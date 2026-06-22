package net.modgarden.gardenbot.database.fixer.fix;

import net.modgarden.gardenbot.database.fixer.DatabaseFix;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.Consumer;

public class V1ToV2 extends DatabaseFix {
	public V1ToV2() {
		super(1);
	}

	@Nullable
	@Override
	public Consumer<Connection> fix(Connection connection) throws SQLException {
		Statement createTeamInvites = connection.createStatement();
		createTeamInvites.execute("""
					CREATE TABLE IF NOT EXISTS team_invites (
						id TEXT UNIQUE NOT NULL,
						user_id TEXT NOT NULL,
						project_id TEXT NOT NULL,
						role TEXT NOT NULL,
						expiration_time INTEGER NOT NULL,
						PRIMARY KEY(id)
					)""");

		Statement createSudoers = connection.createStatement();
		createSudoers.execute("""
					CREATE TABLE IF NOT EXISTS sudoers (
						user_id TEXT UNIQUE NOT NULL,
						expires INTEGER NOT NULL,
						PRIMARY KEY(user_id)
					)""");

		Statement createImageChannels = connection.createStatement();
		createImageChannels.execute("""
					CREATE TABLE IF NOT EXISTS channels (
						id TEXT UNIQUE NOT NULL,
						channel_id TEXT NOT NULL,
						PRIMARY KEY(id)
					)""");

		return null;
	}
}
