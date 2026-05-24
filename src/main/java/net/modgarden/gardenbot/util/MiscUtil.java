package net.modgarden.gardenbot.util;

public class MiscUtil {
	public static String aOrAn(String value) {
		return startsWithVowel(value) ? "an" : "a";
	}

	public static boolean startsWithVowel(String value) {
		return value.startsWith("a")
				|| value.startsWith("e")
				|| value.startsWith("i")
				|| value.startsWith("o")
				|| value.startsWith("u");
	}
}
