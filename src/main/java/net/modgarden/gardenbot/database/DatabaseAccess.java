package net.modgarden.gardenbot.database;

import net.modgarden.gardenbot.GardenBot;
import net.modgarden.gardenbot.database.data.NaturalId;
import net.modgarden.gardenbot.database.data.TeamInvite;
import net.modgarden.gardenbot.util.FallibleSupplier;
import net.modgarden.gardenbot.util.LazyValue;
import net.modgarden.gardenbot.util.TimeUtil;
import org.jetbrains.annotations.Nullable;

import java.sql.*;
import java.time.Instant;

/// Centralized access to database operations.
public final class DatabaseAccess implements AutoCloseable {
	private static final ScopedValue<DatabaseAccess> SCOPED_VALUE = ScopedValue.newInstance();

	private final LazyValue<Connection> connection = LazyValue.of();

	private DatabaseAccess() {
	}

	/// Binds the [ScopedValue] of this [DatabaseAccess] to the current thread.
	///
	/// @return a [ScopedValue.Carrier] which should be used to wrap subsequent calls that need [DatabaseAccess].
	public static ScopedValue.Carrier bind() {
		return ScopedValue.where(SCOPED_VALUE, new DatabaseAccess());
	}

	/// @return the current thread's access to the database. This may differ from other threads.
	public static DatabaseAccess get() {
		return SCOPED_VALUE.orElseThrow(() -> new IllegalStateException("DatabaseAccess is not available in this " +
				"context"));
	}

	/// **Warning:** do not call [Connection#close()] or use it in a try-with-resources as this will prematurely close
	/// the connection.
	private Connection getConnection() throws SQLException {
		return this.connection.getOrCreate(GardenBot::createDatabaseConnection);
	}

	/// @return the value returned by the supplier or the default value if an exception is thrown.
	public <T, X extends Throwable> T logIfThrown(FallibleSupplier<T, X> operation, T defaultValue) {
		try {
			return operation.get();
		} catch (Throwable t) {
			GardenBot.LOG.error("Exception during DatabaseAccess operation", t);
			return defaultValue;
		}
	}

	/// @return the value returned by the supplier or the default value if an exception is thrown.
	public <X extends Throwable> boolean logIfThrown(FallibleSupplier<Boolean, X> operation) {
		return this.logIfThrown(operation, false);
	}

	// Team Invites
	@Nullable
	public TeamInvite getTeamInvite(String id) throws SQLException {
		try (
				var statement = getConnection().prepareStatement("""
						SELECT user_id, project_id, role, expiration_time
						FROM team_invites
						WHERE id = ?""")
		) {
			statement.setString(1, id);
			ResultSet resultSet = statement.executeQuery();

			if (!resultSet.isBeforeFirst()) {
				return null;
			}

			return new TeamInvite(
					id,
					resultSet.getString("user_id"),
					resultSet.getString("project_id"),
					resultSet.getString("role"),
					resultSet.getLong("expiration_time")
			);
		}
	}

	public boolean updateTeamInvite(String userId,
									String projectId,
									String role) throws SQLException {
		try (
				var insertStatement = getConnection().prepareStatement("""
						UPDATE team_invites
						SET expiration_time = ?
						WHERE user_id = ? AND project_id = ? AND role = ?""")
		) {
			insertStatement.setString(1, userId);
			insertStatement.setString(2, projectId);
			insertStatement.setString(3, role);
			return insertStatement.executeUpdate() > 0;
		}
	}

	public String createTeamInvite(String userId,
								   String projectId,
								   String role) throws SQLException {
		try (
				var insertStatement = getConnection().prepareStatement("""
						INSERT INTO team_invites(id, user_id, project_id, role, expiration_time)
						VALUES (?, ?, ?, ?, ?)""")
		) {
			String id = NaturalId.generate("team_invites", "id", null, 5);
			long expirationTime = Instant.now().toEpochMilli() + TimeUtil.DAY_MS;

			insertStatement.setString(1, id);
			insertStatement.setString(2, userId);
			insertStatement.setString(3, projectId);
			insertStatement.setString(4, role);
			insertStatement.setLong(5, expirationTime);
			insertStatement.executeUpdate();

			return id;
		}
	}

	public void revokeTeamInvite(String id) throws SQLException {
		try (var deleteStatement = this.getConnection().prepareStatement("""
					DELETE FROM team_invites
					WHERE id = ?
				""")) {
			deleteStatement.setString(1, id);
			deleteStatement.executeUpdate();
		}
	}

	public int revokeInvalidTeamInvites() throws SQLException {
		try (var deleteStatement = this.getConnection().prepareStatement("""
					DELETE FROM team_invites
					WHERE expiration_time > ?
				""")) {
			deleteStatement.setLong(1, Instant.now().toEpochMilli());
			return deleteStatement.executeUpdate();
		}
	}

	@Override
	public void close() throws Exception {
		this.connection.ifPresent(Connection::close);
	}
}
