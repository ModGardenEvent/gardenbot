import { createEmbeds, iconBigintToHash } from '@discordeno/bot'
import { bot } from '../bot.js'
import { configs } from '../config.js'
import { messageCache } from '../util/messageCache.js'

bot.events.messageUpdate = async ( message ) => {
    if (!configs.moderationLogsChannelId || message.author.bot)
        return

    const oldMessage = messageCache.get(message.id)

    const description = `
        **Channel:** <#${message.channelId}> (${(await bot.helpers.getChannel(message.channelId)).name!}
        **Author:** <@${message.author.id}> (${message.author.username})
        **Author Id:** ${message.author.id}
    `

    bot.helpers.sendMessage(configs.moderationLogsChannelId, { embeds: createEmbeds()
        .setColor('#46883b')
        .setAuthor('Message Edited!', { icon_url: `https://cdn.discordapp.com/avatars/${iconBigintToHash(message.author.avatar!)}` } )
        .setDescription(description)
        .addField('Old Message', oldMessage ? oldMessage.content : "Unknown")
        .addField('New Message', message.content)
        .validate(),
        allowedMentions: {
            parse: []
        }
    })
    messageCache.set(message.id, message)
}