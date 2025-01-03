import ms from 'ms'

import { ApplicationCommandOptionTypes } from '@discordeno/types'
import { createEmbeds, CreateGuildBan, DiscordInteractionContextType, Member, User } from '@discordeno/bot'
import { createCommand } from '../../util/commands.js'
import { closestHour } from '../../util/time.js'
import { bot, logger } from '../../bot.js'
import { recordBan } from '../../util/banDatabase.js'
import { configs } from '../../config.js'

const permaAliases = [ "perma", "permaban", "permanent" ]

createCommand({
    command: {
        name: 'ban',
        description: 'Bans a target user from the Mod Garden Discord.',
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
            },
            {
                name: 'duration',
                description: 'The duration of the ban. (Examples: perma, permanent, 1d, 7d, 28d, 1y)',
                type: ApplicationCommandOptionTypes.String,
                required: true
            },
            {
                name: 'delete_until',
                description: 'The duration to delete all messages before. Has a maximum of 7 days. (Examples: 1d, 4d, 7d)',
                type: ApplicationCommandOptionTypes.String,
                required: false
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
        
        const { user, duration, delete_until, reason } = args as { user: {user: User, member?: Member}; duration: string; delete_until?: string; reason: string  }

        if (!hasPermissions(interaction, interactionMember as Member, user.user.id))
               return

        let msDuration = -1
        const isPermanent = permaAliases.some(alias => duration.toLocaleLowerCase() === alias)
        if (!isPermanent) {
            try {
                msDuration = ms(duration)
            } catch (ignored) {
                interaction.respond(`Invalid duration: '${duration}'.`)
                return
            }
        }


        let deleteUntil = 0
        if (delete_until)
            deleteUntil = ms(delete_until) / 1000
        
        if (deleteUntil > ms('7d') / 1000) {
            await interaction.respond(
                `Cannot delete all of a user's messages as far back as more than 7 days/1 week.`,
                { isPrivate: true } 
            )
        }

        const guildBan = { deleteMessageSeconds: deleteUntil }
        const dmChannel = await interaction.bot.helpers.getDmChannel(user.user.id)
        
        const durString = isPermanent ? 'permanently' : `for ${ms(msDuration, { long: true })}`

        if (!dmChannel || !user.member) {
            await banMember(interaction, user.user, guildBan, msDuration, durString, reason, true)
            return
        }

        const unbanTimeSeconds = Math.floor((closestHour(Date.now()) + closestHour(msDuration)) / 1000)
        const description = isPermanent ? `Reason Specified by Moderators: ${reason}` : `You will be unbanned on <t:${unbanTimeSeconds}:f>\nReason Specified by Moderators: ${reason}`

        try {
            await interaction.bot.helpers.sendMessage(dmChannel.id, { embeds: createEmbeds()
                .setColor('#2ecc71')
                .setAuthor('Mod Garden', { icon_url: "https://cdn.discordapp.com/avatars/1305609404837527612/1539b56c609c6082b6f2179e4e985035.webp" } )
                .setTitle(`You have been banned from the Mod Garden Discord ${durString}.`)
                .setDescription(description)
                .addField('Ban Appeal Forum', 'You may request an appeal through [this link.](https://docs.google.com/forms/d/e/1FAIpQLSfWz-XskMEuF-26nuiS2UICu1YHYnAjZXEdrZaNjp_EGRRKyQ/viewform?usp=sf_link)')
                .validate()
            })
            await banMember(interaction, user.user, guildBan, msDuration, durString, reason, false)
        } catch (ex) {
            logger.error(`Failed to send DM to user ${user.user.username}. ${ex}`)
            await banMember(interaction, user.user, guildBan, msDuration, durString, reason, true)
        }
    }
})

async function banMember(interaction: typeof bot.transformers.$inferredTypes.interaction, user: typeof bot.transformers.$inferredTypes.user, guildBan: CreateGuildBan, duration: number, durString: string, reason: string, failedDm: boolean) {
    recordBan(user.id, user.username, duration, reason)
    try {
        await interaction.bot.helpers.banMember(configs.guildId, user.id, guildBan, reason)
    } catch (ex) {
        logger.error(`Could not ban user ${user.username} from Mod Garden.`)
    }

    logger.info(`User ${user.username} has been banned by ${interaction.user.username}.`)
    let message = `Successfully banned user ${user.username} ${durString}.
    \nFor reason: ${reason}`
    if (failedDm) {
        message = message + `\nFailed to DM user about this ban.`
    }

    if (configs.moderationLogsChannelId) {
        bot.helpers.sendMessage(configs.moderationLogsChannelId, { embeds: createEmbeds()
            .setColor('#46883b')
            .setAuthor('Banned User!', { icon_url: `https://cdn.discordapp.com/avatars/1305609404837527612/1539b56c609c6082b6f2179e4e985035.webp` } )
            .setDescription(`
                **Username:** ${user.username}
                **User ID:** ${user.id}
            `)
            .addField("Reason", reason)
            .addField("Duration", durString)
            .validate(),
            allowedMentions: {
                parse: []
            }
        })
    }

    await interaction.respond(
        message,
        { isPrivate: true }
    )
}

async function hasPermissions(interaction: typeof bot.transformers.$inferredTypes.interaction, sender: Member, userId: bigint) : Promise<boolean> {
    if (userId == interaction.bot.id) {
        await interaction.respond(
            `I cannot ban myself.`,
            { isPrivate: true }
        )
        return false
    }
    
    const guildId = configs.guildId

    let target
    try {
        target = await interaction.bot.helpers.getMember(guildId, userId)
    } catch (error) {
        return true
    }

    let botMember 
    try {
        botMember = await interaction.bot.helpers.getMember(guildId, interaction.bot.id)
    } catch (error) {
        return true
    }

    let guild
    try {
        guild = await interaction.bot.helpers.getGuild(guildId)
    } catch (error) {
        bot.logger.error(`Could not find target guild ${guildId}.`)
        await interaction.respond(
            `Could not find target guild. This should not happen!
            \nPlease report this to a developer of the bot.`,
            { isPrivate: true }
        )
        return false
    }

    const targetPermissions = getMaxRolePosition(target, guild)
    const senderPermissions = getMaxRolePosition(sender, guild)
    const botPermissions = getMaxRolePosition(botMember, guild)

    if (targetPermissions >= senderPermissions) {
        await interaction.respond(
            `Cannot ban a user with the same or higher permissions in guild ${guild.name}.`,
            { isPrivate: true }
        )
        return false
    }
    if (targetPermissions >= botPermissions) {
        await interaction.respond(
            `Cannot ban a user with the same or higher permissions than the bot in guild ${guild.name}.`,
            { isPrivate: true }
        )
        return false
    }

    return true
}

function getMaxRolePosition(target: typeof bot.transformers.$inferredTypes.member, guild: typeof bot.transformers.$inferredTypes.guild) : number {
    if (!target.roles)
        return 0
    return Math.max(...target.roles.map(roleId => {
        const role = guild.roles.get(roleId)
        if (!role)
            return 0
        return role.position
    }))
}