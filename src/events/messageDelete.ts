import { createEmbeds, iconBigintToHash } from "@discordeno/bot"
import { bot } from "../bot.js"
import { configs } from "../config.js"
import { getMessage, deleteMessage } from "../util/messageDatabase.js"

bot.events.messageDelete = async ( payload, message ) => {
    if (!configs.moderationLogsChannelId)
        return

    if (payload.guildId != configs.guildId)
        return

    const msg = await getMessage(payload.id)
    if (!msg)
        return
    await deleteMessage(payload.id)
    const author = await bot.helpers.getMember(payload.guildId, msg.user_id)

    if (!author || !author.user)
        return

    const channel = await bot.helpers.getChannel(payload.channelId)
    const description = `
        **Channel:** <#${payload.channelId}> (${channel.name})
        **Author:** <@${author.user.id}> (${author.user.username})
        **Author ID:** ${author.id}
    `
    const avatarHash = iconBigintToHash(author.user.avatar!);

    await bot.helpers.sendMessage(configs.moderationLogsChannelId, { embeds: createEmbeds()
        .setColor('#873a3a')
        .setAuthor('Message Deleted!', { icon_url: `https://cdn.discordapp.com/avatars/${author.id}/${avatarHash}.webp` } )
        .setDescription(description)
        .addField('Contents', msg.content)
        .validate(),
        allowedMentions: {
            parse: []
        }
    })
}