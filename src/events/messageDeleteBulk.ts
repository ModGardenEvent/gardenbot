import { bot } from "../bot.js"
import { configs } from "../config.js";
import { deleteMessage } from "../util/messageDatabase.js"

bot.events.messageDeleteBulk = async ( payload ) => {
    if (!configs.moderationLogsChannelId || payload.guildId != configs.guildId)
        return
    payload.ids.forEach((id) => {
        deleteMessage(id)
    })
}