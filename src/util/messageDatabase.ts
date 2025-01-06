import { logger } from '../bot.js'
import { closestStartOfDay, twentyFourHours } from "./time.js"
import Database from 'better-sqlite3'

const db = new Database('./database.db')

export async function createMessageCacheTable() {
    db.exec('CREATE TABLE IF NOT EXISTS message_cache (message_id TEXT PRIMARY KEY, user_id TEXT, content TEXT, removal_timestamp TEXT)') 
}

export async function cacheMessage(key: bigint, authorId: bigint, content: string) {
    await createMessageCacheTable()
    const removalTimestamp = closestStartOfDay(Date.now()) + (twentyFourHours * 10)
    db.exec(`INSERT INTO message_cache (message_id, user_id, content, removal_timestamp) VALUES ('${key}', '${authorId}', '${content}', '${removalTimestamp}') ON CONFLICT(message_id) DO UPDATE SET content = '${content}', removal_timestamp = '${removalTimestamp}'`)
}

export async function getMessage(key: bigint) {
    const result = db.prepare('SELECT user_id, content FROM message_cache WHERE CAST(message_id AS BIGINT) == ?').get(key)
    if (!result)
        return null
    return result as User
}

export async function deleteMessage(key: bigint) {
    const result = db.prepare('DELETE FROM message_cache WHERE CAST(message_id AS BIGINT) == ?').run(key)
    if (result.changes == 0)
        logger.info(`Message ${key} was not in database.db.`)
    else
        logger.info(`Successfully removed key of message ${key} from database.db.`)
}

export interface User {
    user_id: string,
    content: string
}

export async function removeExpiredMessages() {
    logger.info(`Attempting to remove expired messages...`)
    const currentTime = closestStartOfDay(Date.now())

    const removed = db.prepare(`DELETE FROM message_cache WHERE CAST(removal_timestamp AS BIGINT) <= ?`).run(currentTime)
    if (removed.changes > 0) {
        logger.info(`Successfuly removed ${removed.changes} message(s) from database.db.`)
    } else 
        logger.info('Did not remove any messages from cache.')
    
}