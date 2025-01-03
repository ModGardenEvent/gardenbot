import { createBot, createDesiredPropertiesObject, Intents } from '@discordeno/bot'
import { configs } from './config.js'

export const desiredProperties = createDesiredPropertiesObject({
    channel: {
        id: true,
        name: true
    },
    guild: {
        channels: true,
        id: true,
        name: true,
        roles: true
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
        guildId: true,
        id: true,
        roles: true,
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
    role: {
        position: true
    },
    user: {
        avatar: true,
        id: true,
        toggles: true,
        username: true
    }
})

interface BotDesiredProperties extends Required<typeof desiredProperties> {}

export const bot = createBot<BotDesiredProperties>({
    token: configs.token,
    intents: Intents.Guilds | Intents.GuildMembers | Intents.GuildMessages | Intents.MessageContent,
    desiredProperties: desiredProperties
})
export const user = (await bot.helpers.getUser(bot.id))

export const logger = bot.logger