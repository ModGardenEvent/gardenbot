import { Collection, CreateApplicationCommand, Interaction } from "@discordeno/bot";
import { bot } from "../bot";

export const commands = new Collection<string, Command>()


export function createCommand(command: Command): void {
    commands.set(command.command.name, command)
}

export interface Command {
    command: CreateApplicationCommand,
    execute: (interaction: typeof bot.transformers.$inferredTypes.interaction, args: Record<string, unknown>) => unknown
}