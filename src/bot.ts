import { createBot, Intents } from '@discordeno/bot'
import { configs } from './config.js'

export const bot = createBot({
    token: configs.token,
    intents: Intents.Guilds | Intents.GuildMessages | Intents.MessageContent,
    desiredProperties: {
        channel: {
            id: true
        },
        guild: {
            channels: true,
            id: true,
            name: true
        },
        interaction: {
            channelId: true,
            data: true,
            id: true,
            guild: true,
            guildId: true,
            member: true,
            message: true,
            token: true,
            type: true,
            user: true
        },
        interactionResource: {
            type: true,
            activityInstance: true,
            message: true
        },
        member: {
            id: true,
            user: true
        },
        message: {
            activity: true,
            author: true,
            channelId: true,
            content: true,
            editedTimestamp: true,
            guildId: true,
            id: true
        },
        user: {
            avatar: true,
            id: true,
            toggles: true,
            username: true
        }
    }
})
export const user = (await bot.helpers.getUser(bot.id))

export const logger = bot.logger