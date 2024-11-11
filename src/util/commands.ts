import { Collection, CreateApplicationCommand, Interaction } from "@discordeno/bot";

export const commands = new Collection<string, Command>()


export function createCommand(command: Command): void {
    commands.set(command.command.name, command)
}

export interface Command {
    command: CreateApplicationCommand,
    execute: (interaction: Interaction, args: Record<string, unknown>) => unknown
}