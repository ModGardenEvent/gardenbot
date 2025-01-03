import { ApplicationCommandOptionTypes } from '@discordeno/types'
import { createEmbeds,  DiscordInteractionContextType, Interaction, Member, User } from '@discordeno/bot'
import { createCommand } from '../../util/commands.js'
import { bot, logger } from '../../bot.js'
import { configs } from '../../config.js'
import { unrecordBan } from '../../util/banDatabase.js'

createCommand({
    command: {
        name: 'unban',
        description: 'Unbans a target user from the Mod Garden Discord.',
        defaultMemberPermissions: ["BAN_MEMBERS"],
        contexts: [ DiscordInteractionContextType.Guild ],
        options: [
            {
                name: 'user',
                description: 'The target user to ban.',
                type: ApplicationCommandOptionTypes.User,
                required: true
            },
            {
                name: 'reason',
                description: 'The reason for the ban.',
                type: ApplicationCommandOptionTypes.String,
                required: true
            }
        ]
    },
    execute: async function (interaction: typeof bot.transformers.$inferredTypes.interaction, args: Record<string, unknown>) {
        await interaction.defer(true)
        
        const interactionMember = interaction.member
        if (!interactionMember) {
            interaction.respond(`Cannot ban a user without a member behind the ban.`)
            return
        }
        
        const { user, reason } = args as { user: {user: User, member?: Member}; reason: string  }

        unbanMember(interaction, user.user, reason)
    }
})

async function unbanMember(interaction: typeof bot.transformers.$inferredTypes.interaction, user: User, reason: string) {
    unrecordBan(user.id)
    try {
        await interaction.bot.helpers.unbanMember(configs.guildId, user.id, reason)
    } catch (ex) {
        logger.error(`Could not unban user ${user.username} from Mod Garden.`)
        interaction.respond(`Could not unban user ${user.username} from Mod Garden. Please check if they are actually banned.`)
        return
    }

    if (configs.moderationLogsChannelId) {
        bot.helpers.sendMessage(configs.moderationLogsChannelId, { embeds: createEmbeds()
            .setColor('#46883b')
            .setAuthor('Unbanned User!', { icon_url: `https://cdn.discordapp.com/avatars/1305609404837527612/1539b56c609c6082b6f2179e4e985035.webp` } )
            .setDescription(`
                **Username:** ${user.username}
                **User ID:** ${user.id}
            `)
            .addField("Reason", reason)
            .validate(),
            allowedMentions: {
                parse: []
            }
        })
    }

    await interaction.respond(
        `Successfully unbanned user ${user.username}.\nFor reason: ${reason}`,
        { isPrivate: true }
    )
}