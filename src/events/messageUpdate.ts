import { createEmbeds, iconBigintToHash } from '@discordeno/bot'
import { bot } from '../bot.js'
import { configs } from '../config.js'
import { cacheMessage, getMessage } from '../util/messageDatabase.js'

bot.events.messageUpdate = async ( message ) => {
    if (!configs.moderationLogsChannelId || message.author.bot || message.guildId != configs.guildId)
        return

    const oldMessage = await getMessage(message.id)
    if (!oldMessage)
        return

    const description = `
        **Channel:** <#${message.channelId}>)
        **Author:** <@${message.author.id}> (${message.author.username})
        **Author ID:** ${message.author.id}
    `
    const avatarHash = iconBigintToHash(message.author.avatar!);

    await bot.helpers.sendMessage(configs.moderationLogsChannelId, { embeds: createEmbeds()
        .setColor('#46883b')
        .setAuthor('Message Edited!', { icon_url: `https://cdn.discordapp.com/avatars/${message.author.id}/${avatarHash}.webp` } )
        .setDescription(description)
        .addField('Old Message', oldMessage.content)
        .addField('New Message', message.content)
        .validate(),
        allowedMentions: {
            parse: []
        }
    })
    await cacheMessage(message.id, message.author.id, message.content)
}