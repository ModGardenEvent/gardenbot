package net.modgarden.gardenbot.util;

import java.util.Locale;

public class StringUtil {
	public static String aOrAn(String value) {
		return startsWithVowel(value) ? "an" : "a";
	}

	public static boolean startsWithVowel(String value) {
		String lowercase = value.toLowerCase(Locale.ROOT);
		return lowercase.startsWith("a")
				|| lowercase.startsWith("e")
				|| lowercase.startsWith("i")
				|| lowercase.startsWith("o")
				|| lowercase.startsWith("u");
	}
}
