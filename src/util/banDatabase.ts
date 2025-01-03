import { createEmbeds } from '@discordeno/bot'
import { closestStartOfDay } from './time.js'
import { bot, logger } from '../bot.js'
import { configs } from '../config.js'
import { botName } from '../constants.js'
import Database from 'better-sqlite3'

const db = new Database('./database.db')

async function createBanTable() {
    db.exec('CREATE TABLE IF NOT EXISTS bans (user_id TEXT NOT NULL UNIQUE PRIMARY KEY, username TEXT NOT NULL, unban_time TEXT NOT NULL, reason TEXT NOT NULL)') 
}

async function hasTable() {
    if (db.prepare(`SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'bans'`).all.length == 0)
        return false
    return true
}

export function isBanned(userId: bigint) : boolean {
    const checkForBan = db.prepare(`SELECT 1 FROM bans WHERE user_id = ?`)
    const result = checkForBan.all(userId)
    return result.length > 0;
}

export async function recordBan(userId: bigint, username: string, duration: number, reason: string) {
    await createBanTable()
    const unbanTime = duration == -1 ? -1 : closestStartOfDay(Date.now() + duration)
    db.exec(`INSERT INTO bans(user_id, username, unban_time, reason) VALUES ('${userId}', '${username}', '${unbanTime}', '${reason}') ON CONFLICT(user_id) DO UPDATE SET username = '${username}', unban_time = '${unbanTime}', reason = '${reason}'`)
    logger.info(`Successfully recorded ban of user ${username} (${userId}) into database.db.`)
}

export async function unrecordBan(userId: bigint) {
    if (!hasTable()) {
        logger.info(`Banned user ${userId} was not in database.db.`)
        return
    }
    const result = db.prepare('DELETE FROM bans WHERE CAST(user_id AS BIGINT) == ?').run(userId);
    if (result.changes == 0)
        logger.info(`Banned user ${userId} was not in database.db.`)
    else
        logger.info(`Successfully unrecorded ban of user ${userId} from database.db.`)
}

export async function unbanExpiredBans() {
    if (!hasTable()) {
        logger.info(`No bans to expire.`)
        return
    }
    logger.info(`Attempting to unban users...`)
    const currentTime = closestStartOfDay(Date.now())

    const usersToUnbanStatement = db.prepare(`SELECT user_id, username FROM bans WHERE CAST(unban_time AS BIGINT) <= ? AND CAST(unban_time AS BIGINT) != -1`)
    const toUnban = usersToUnbanStatement.all(currentTime)

    let unbanned = new Set<string>()
    for (const value of toUnban) {
        if (!isUnbannable(value)) {
            logger.error('Specified user to unban is incorrectly typed, this should not happen. (Skipping).')
            continue
        }
        const unbannable = value as Unbannable
        logger.info(`Attempting to unban user: ${unbannable.user_id}...`)

        try {
            await bot.helpers.unbanMember(configs.guildId, unbannable.user_id, `${botName} ban duration has expired.`)
            logger.info(`Unbanned user ${unbannable.user_id} as their ${botName} ban has expired.`)
            unbanned.add(`${unbannable.username} (${unbannable.user_id})`)
        } catch (error) {
            logger.warn(`Failed to unban user ${unbannable.user_id} from Mod Garden.`)
        }
    }
    db.prepare(`DELETE FROM bans WHERE CAST(unban_time AS BIGINT) <= ? AND CAST(unban_time AS BIGINT) != -1`).run(currentTime)
    if (unbanned.size > 0) {
        logger.info(`Successfuly unbanned ${unbanned.size} user(s).`)

        if (!configs.moderationLogsChannelId) 
            return

        const description = Array.from(unbanned).join(',\n')

        bot.helpers.sendMessage(configs.moderationLogsChannelId, { embeds: createEmbeds()
            .setColor('#46883b')
            .setAuthor('Unbanned Users!', { icon_url: `https://cdn.discordapp.com/avatars/1305609404837527612/1539b56c609c6082b6f2179e4e985035.webp` } )
            .setDescription(description)
            .validate(),
            allowedMentions: {
                parse: []
            }
        })
    } else 
        logger.info('Did not unban any users.')
}

function isUnbannable(obj: any): obj is Unbannable {
    return typeof obj.user_id === 'string'
}

interface Unbannable {
    user_id: string,
    username: string
}