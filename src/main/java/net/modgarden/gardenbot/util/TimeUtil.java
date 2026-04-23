package net.modgarden.gardenbot.util;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TimeUtil {
	public static final long HOUR_MS = 3600000;
	public static final long DAY_MS = HOUR_MS * 24;
	public static final long WEEK_MS = DAY_MS * 7;

	public static void runEachHour(Runnable runnable) {
		long now = System.currentTimeMillis();

		long etaMs = HOUR_MS - now % HOUR_MS;

		try (ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor()) {
			service.scheduleAtFixedRate(runnable, etaMs, HOUR_MS, TimeUnit.MILLISECONDS);
		}
	}
}
