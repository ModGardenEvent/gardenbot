package net.modgarden.gardenbot.client;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.modgarden.gardenbot.GardenBot;
import net.modgarden.gardenbot.client.exception.HypertextException;
import net.modgarden.gardenbot.client.mod_garden.role.ModGardenRole;
import net.modgarden.gardenbot.client.mod_garden.role.integration.DiscordRoleIntegration;
import net.modgarden.gardenbot.client.mod_garden.user.ModGardenUser;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static net.modgarden.gardenbot.GardenBot.HTTP_CLIENT;

public class Discord {
	private static final String USER_AGENT = "ModGardenEvent/gardenbot/" + GardenBot.VERSION + " (modgarden.net)";

	/// Hack to avoid Discord converting PNGs to WEBP when uploading an attachment.
	///
	/// @param attachment The uploaded attachment.
	@Nullable
	public static InputStream attachmentToPngStream(Message.Attachment attachment) throws HypertextException {
		try {
			if (attachment.getContentType() != null && attachment.getContentType().equals("image/webp")) {
				HttpResponse<InputStream> response = HTTP_CLIENT.send(
						HttpRequest.newBuilder(URI.create(attachment.getProxyUrl() + "&format=png"))
								.header("User-Agent", USER_AGENT)
								.build(),
						HttpResponse.BodyHandlers.ofInputStream()
				);
				return response.body();
			}
			return attachment.getProxy().download().get();
		} catch (ExecutionException | InterruptedException | IOException e) {
			throw new HypertextException(500, e.getMessage());
		}
	}

	public static List<Role> addModGardenRolesToDiscordUser(Guild guild, Member member) throws HypertextException {
		ModGardenUser modGardenUser = ModGarden.getUserByDiscordUser(member.getUser());

		if (modGardenUser == null)
			return Collections.emptyList();

		List<Role> rolesToAdd = new ArrayList<>(member.getRoles());

		for (String roleId : modGardenUser.roles()) {
			try {
				ModGardenRole modGardenRole = ModGarden.getUserRole(roleId);
				if (modGardenRole == null)
					continue;

				DiscordRoleIntegration integration = modGardenRole.integrations().discord();
				if (integration == null)
					continue;

				Role discordRole = guild.getRoleById(integration.roleId());
				if (discordRole == null)
					continue;

				rolesToAdd.add(discordRole);
			} catch (HypertextException e) {
				GardenBot.LOG.error("", e);
			}
		}

		guild.modifyMemberRoles(member, rolesToAdd).queue();
		return rolesToAdd;
	}
}
