package net.modgarden.gardenbot.commands.event;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.modgarden.gardenbot.GardenBot;
import net.modgarden.gardenbot.interaction.SlashCommandInteraction;
import net.modgarden.gardenbot.interaction.response.EmbedResponse;
import net.modgarden.gardenbot.interaction.response.Response;
import net.modgarden.gardenbot.util.ModGardenAPIClient;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SubmitHandler {
	public static Response handleSubmit(SlashCommandInteraction interaction) {
		interaction.event().deferReply(true).queue();
		User user = interaction.event().getUser();

		JsonObject inputJson = new JsonObject();
		inputJson.addProperty("discord_id", user.getId());

		@Nullable String event = interaction.event().getOption("event", OptionMapping::getAsString);
		if (event != null)
			inputJson.addProperty("event", event);

		String slug = interaction.event().getOption("slug", OptionMapping::getAsString);
		inputJson.addProperty("slug", slug);

		String source = interaction.event().getOption("source", OptionMapping::getAsString);
		if (!"modrinth".equals(source)) {
			return new EmbedResponse()
					.setTitle("Could not submit your project to Mod Garden.")
					.setDescription("Invalid mod source.")
					.setColor(0x5D3E40);
		}

		try {
			HttpResponse<InputStream> stream = ModGardenAPIClient.post(
					"discord/submission/create/" + source,
					HttpRequest.BodyPublishers.ofString(inputJson.toString()),
					HttpResponse.BodyHandlers.ofInputStream(),
					"Authorization", "Basic " + GardenBot.DOTENV.get("OAUTH_SECRET"),
					"Content-Type", "application/json"
			);
			JsonElement json = JsonParser.parseReader(new InputStreamReader(stream.body()));
			if (stream.statusCode() == 401 || stream.statusCode() == 422) {
				String errorDescription = json.isJsonObject() && json.getAsJsonObject().has("description") ?
						json.getAsJsonObject().getAsJsonPrimitive("description").getAsString() :
						"Undefined Error.";
				return new EmbedResponse()
						.setTitle("Could not submit your project to Mod Garden.")
						.setDescription(errorDescription)
						.setColor(0x5D3E40);
			} else if (stream.statusCode() < 200 || stream.statusCode() > 299) {
				String errorDescription = json.isJsonObject() && json.getAsJsonObject().has("description") ?
						json.getAsJsonObject().getAsJsonPrimitive("description").getAsString() :
						"Undefined Error.";
				return new EmbedResponse()
						.setTitle("Encountered an exception whilst attempting to submit your project to Mod Garden.")
						.setDescription(stream.statusCode() + ": " + errorDescription + "\nPlease report this to a team member.")
						.setColor(0xFF0000);
			}

			String resultDescription = json.isJsonObject() && json.getAsJsonObject().has("description") ?
					json.getAsJsonObject().getAsJsonPrimitive("description").getAsString() :
					"Undefined Result.";
			return new EmbedResponse()
					.setTitle(resultDescription)
					.setColor(0xA9FFA7);
		} catch (IOException | InterruptedException ex) {
			GardenBot.LOG.error("", ex);
			return new EmbedResponse()
					.setTitle("Encountered an exception whilst attempting to submit your project to Mod Garden.")
					.setDescription(ex.getMessage() + "\nPlease report this to a team member.")
					.setColor(0xFF0000);
		}
	}

	public static List<Command.Choice> getChoices(String focusedOption)  {
		if (focusedOption.equals("slug")) {
			return Collections.emptyList();
		}
		if (focusedOption.equals("source")) {
			return List.of(new Command.Choice("Modrinth", "modrinth"));
		}
		try {
			var activeEventsResult = ModGardenAPIClient.get("events/active", HttpResponse.BodyHandlers.ofInputStream());
			if (activeEventsResult.statusCode() == 200) {
				List<Command.Choice> choices = new ArrayList<>();
				try (InputStreamReader activeEventsReader = new InputStreamReader(activeEventsResult.body())) {
					JsonElement activeEventsJson = JsonParser.parseReader(activeEventsReader);
					if (activeEventsJson.isJsonArray()) {
						for (JsonElement eventJson : activeEventsJson.getAsJsonArray()) {
							if (!eventJson.isJsonObject())
								continue;
							choices.add(new Command.Choice(eventJson.getAsJsonObject().get("display_name").getAsString(), eventJson.getAsJsonObject().get("slug").getAsString()));
						}
					}
				}
				return choices;
			}
		} catch (IOException | InterruptedException ex) {
			GardenBot.LOG.error("Could not get active events.", ex);
		}
		return Collections.emptyList();
	}
}
