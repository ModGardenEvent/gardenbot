package net.modgarden.gardenbot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.util.EnumSet;

public class GardenBot {
	public static JDA jda;

	public static void main(String[] args) {
		jda =  JDABuilder.create(EnumSet.noneOf(GatewayIntent.class))
				.setToken(System.getenv("GARDEN_BOT_TOKEN"))
				.addEventListeners(new GardenBotEvents())
				.build();
    }
}
