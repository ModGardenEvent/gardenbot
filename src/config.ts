const token = process.env.TOKEN
const guildId = process.env.GUILD_ID

if (!token) throw new Error('Missing TOKEN environment variable');
if (!guildId) throw new Error('Missing GUILD_ID environment variable');

export const configs: Config = {
    token,
    guildId: BigInt(guildId)
}

export interface Config {
    token: string
    guildId: bigint
}