import { bot } from "../bot.js"
import { configs } from "../config.js";
import { messageCache } from "../util/messageCache.js"

bot.events.messageDeleteBulk = async ( payload ) => {
    if (payload.guildId != configs.guildId)
        return
    payload.ids.forEach((id) => {
        messageCache.delete(id)
    })
}