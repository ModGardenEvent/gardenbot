import { createEmbeds, iconBigintToHash } from '@discordeno/bot'
import { bot } from '../bot.js'
import { configs } from '../config.js'
import { isBanned } from '../util/banDatabase.js'

bot.events.guildMemberRemove = async ( user, guildId ) => {
    if (!configs.joinLogsChannelId || guildId != configs.guildId || isBanned(user.id))
        return

    const description = `
        Goodbye <@${user.id}>.
        **ID:** ${user.id}
    `
    const avatarHash = iconBigintToHash(user.avatar!);
    
    await bot.helpers.sendMessage(configs.joinLogsChannelId, { embeds: createEmbeds()
        .setColor('#46883b')
        .setAuthor('User left the server!', { icon_url: `https://cdn.discordapp.com/avatars/${user.id}/${avatarHash}.webp` } )
        .setDescription(description)
        .validate(),
        allowedMentions: {
            parse: []
        }
    })
}