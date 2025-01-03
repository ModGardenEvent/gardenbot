import { createEmbeds, iconBigintToHash } from '@discordeno/bot'
import { bot } from '../bot.js'
import { configs } from '../config.js'

bot.events.guildMemberAdd = async ( member, user ) => {
    if (!configs.joinLogsChannelId || member.guildId != configs.guildId)
        return

    const description = `
        Welcome <@${user.id}>. 
        **ID:** ${user.id}
    `
    const avatarHash = iconBigintToHash(user.avatar!);
    
    await bot.helpers.sendMessage(configs.joinLogsChannelId, { embeds: createEmbeds()
        .setColor('#46883b')
        .setAuthor('User joined the server!', { icon_url: `https://cdn.discordapp.com/avatars/${user.id}/${avatarHash}.webp` } )
        .setDescription(description)
        .validate(),
        allowedMentions: {
            parse: []
        }
    })
}