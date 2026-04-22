package net.modgarden.gardenbot.util;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TimeUtil {
	public static final long DAY_MS = 86400000;

	public static void runEachHour(Runnable runnable) {
		long now = System.currentTimeMillis();
		Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

		time.set(Calendar.HOUR, time.get(Calendar.HOUR) + 1);
		time.set(Calendar.MINUTE, 0);
		time.set(Calendar.SECOND, 0);
		time.set(Calendar.MILLISECOND, 0);

		long etaMs = time.getTimeInMillis() - now;

		try (ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor()) {
			service.scheduleAtFixedRate(runnable, etaMs, 1, TimeUnit.HOURS);
		}
	}
}
