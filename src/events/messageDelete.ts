import { createEmbeds, iconBigintToHash } from "@discordeno/bot"
import { bot } from "../bot.js"
import { configs } from "../config.js"
import { messageCache } from "../util/messageCache.js"

bot.events.messageDelete = async ( payload, message ) => {
    if (!configs.moderationLogsChannelId || !message || message.author.bot || payload.guildId != configs.guildId)
        return
    messageCache.delete(message.id)

    const description = `
        **Channel:** <#${message.channelId}> (${(await bot.helpers.getChannel(message.channelId)).name!}
        **Author:** <@${message.author.id}> (${message.author.username})
        **Author Id:** ${message.author.id}
    `

    bot.helpers.sendMessage(configs.moderationLogsChannelId, { embeds: createEmbeds()
        .setColor('#873a3a')
        .setAuthor('Message Deleted!', { icon_url: `https://cdn.discordapp.com/avatars/${iconBigintToHash(message.author.avatar!)}` } )
        .setDescription(description)
        .addField('Contents', message.content)
        .validate(),
        allowedMentions: {
            parse: []
        }
    })
}