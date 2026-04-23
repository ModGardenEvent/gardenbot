package net.modgarden.gardenbot.command.submit;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.modgarden.gardenbot.GardenBot;
import net.modgarden.gardenbot.command.SlashCommand;
import net.modgarden.gardenbot.command.SlashCommandOption;
import net.modgarden.gardenbot.interaction.SlashCommandInteraction;
import net.modgarden.gardenbot.response.EmbedResponse;
import net.modgarden.gardenbot.response.Response;
import net.modgarden.gardenbot.util.ModGardenAPIClient;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

// TODO: Rewrite for Backend V2.
public class SubmitModrinthCommand extends SlashCommand {
	public SubmitModrinthCommand() {
		super(
				"modrinth",
				"Submit your Modrinth project to a current Mod Garden event.",
				new SlashCommandOption(
						OptionType.STRING,
						"slug",
						"The slug of the project to submit.",
						true
				)
		);
	}

	@NotNull
	@Override
	public Response respond(SlashCommandInteraction interaction) {
		interaction.event().deferReply(true).queue();
		User user = interaction.event().getUser();

		JsonObject inputJson = new JsonObject();
		inputJson.addProperty("discord_id", user.getId());

		String slug = interaction.event().getOption("slug", OptionMapping::getAsString);
		inputJson.addProperty("slug", slug);

		try {
			HttpResponse<InputStream> stream = ModGardenAPIClient.post(
					"discord/submission/create/modrinth",
					HttpRequest.BodyPublishers.ofString(inputJson.toString()),
					HttpResponse.BodyHandlers.ofInputStream(),
					"Content-Type", "application/json"
			);
			JsonElement json = JsonParser.parseReader(new InputStreamReader(stream.body()));

			if (stream.statusCode() == 401 || stream.statusCode() == 422) {
				String errorDescription = json.isJsonObject() && json.getAsJsonObject().has("description")
						? json.getAsJsonObject().getAsJsonPrimitive("description").getAsString()
						: "Undefined Error.";
				return new EmbedResponse()
						.setTitle("Could not submit your project to Mod Garden.")
						.setDescription(errorDescription)
						.setColor(0x5D3E40);
			}

			if (stream.statusCode() < 200 || stream.statusCode() > 299) {
				String errorDescription = json.isJsonObject() && json.getAsJsonObject().has("description") ?
						json.getAsJsonObject().getAsJsonPrimitive("description").getAsString() :
						"Undefined Error.";
				return new EmbedResponse()
						.setTitle("Encountered an exception whilst attempting to submit your project to Mod Garden.")
						.setDescription(stream.statusCode() + ": " + errorDescription + "\nPlease report this to a team member.")
						.setColor(0xFF0000);
			}

			String resultDescription = json.isJsonObject() && json.getAsJsonObject().has("description")
					? json.getAsJsonObject().getAsJsonPrimitive("description").getAsString()
					: "Undefined Result.";

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
}
