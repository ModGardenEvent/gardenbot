package net.modgarden.gardenbot.client.modrinth;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public record ModrinthVersion(String id,
                              List<String> loaders,
                              @SerializedName("game_versions") List<String> gameVersions,
                              @SerializedName("date_published") String datePublished,
							  List<ModrinthFile> files) {
}
