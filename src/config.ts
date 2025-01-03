import path from "path";
import dotenv from "dotenv"
import { fileURLToPath } from "url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

dotenv.config({path: path.resolve(__dirname, "../.env")});

const token = process.env.TOKEN
const guildId = process.env.GUILD_ID
const moderationLogsChannelId = process.env.MODERATION_LOGS_CHANNEL_ID
const joinLogsChannelId = process.env.JOIN_LOGS_CHANNEL_ID

if (!token) throw new Error('Missing TOKEN environment variable');
if (!guildId) throw new Error('Missing GUILD_ID environment variable');

export const configs: Config = {
    token,
    guildId: BigInt(guildId),
    moderationLogsChannelId: !moderationLogsChannelId ? null : BigInt(moderationLogsChannelId),
    joinLogsChannelId: !joinLogsChannelId ? null : BigInt(joinLogsChannelId)
}

export interface Config {
    token: string
    guildId: bigint
    moderationLogsChannelId: bigint | null
    joinLogsChannelId: bigint | null
}