import { bot } from '../bot.js'
import { configs } from '../config.js'
import { cacheMessage } from '../util/messageDatabase.js'

bot.events.messageCreate = async ( message ) => {
    if (!configs.moderationLogsChannelId || message.author.bot || message.guildId != configs.guildId || message.content.length == 0)
        return
    await cacheMessage(message.id, message.author.id, message.content)
}