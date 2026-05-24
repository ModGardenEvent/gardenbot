package net.modgarden.gardenbot.util;

import net.modgarden.gardenbot.GardenBot;
import net.modgarden.gardenbot.database.DatabaseAccess;

import java.sql.SQLException;

public class TeamInviteUtil {
	public static void revokeExpiredInvitesEachHour() {
		TimeUtil.runEachHour(() -> DatabaseAccess.bind().run(() -> {
			try {
				DatabaseAccess db = DatabaseAccess.get();
				int updated = db.revokeInvalidTeamInvites();
				GardenBot.LOG.info("Successfully revoked {} expired team invites.", updated);
			} catch (SQLException e) {
				GardenBot.LOG.error("Failed to revoke expired invites. ", e);
			}
		}));
	}
}
