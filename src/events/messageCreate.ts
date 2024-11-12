import { bot } from '../bot.js'
import { configs } from '../config.js'
import { messageCache } from '../util/messageCache.js'

bot.events.messageCreate = async ( message ) => {
    if (!configs.moderationLogsChannelId || message.author.bot || message.guildId != configs.guildId)
        return
    messageCache.set(message.id, message)
}