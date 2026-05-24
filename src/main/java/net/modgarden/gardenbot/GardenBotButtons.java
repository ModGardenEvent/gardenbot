package net.modgarden.gardenbot;

import net.modgarden.gardenbot.button.team.AcceptTeamInviteButton;
import net.modgarden.gardenbot.button.team.DeclineTeamInviteButton;

import static net.modgarden.gardenbot.interaction.dispatcher.ButtonDispatcher.register;

public class GardenBotButtons {
	public static final AcceptTeamInviteButton ACCEPT_TEAM_INVITE = register(AcceptTeamInviteButton::new);
	public static final DeclineTeamInviteButton DECLINE_TEAM_INVITE = register(DeclineTeamInviteButton::new);

	public static void registerAll() {
	}
}
