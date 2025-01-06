import { bot, logger } from './bot.js'
import { botName } from './constants.js'
import { createBanTable, unbanExpiredBans } from './util/banDatabase.js'
import importDirectory from './util/loader.js'
import { runEachBeginningOfDay, runEachHour } from './util/time.js'
import { createMessageCacheTable, removeExpiredMessages } from './util/messageDatabase.js'

logger.info(`Starting ${botName}...`)

logger.info(`Loading ${botName} commands...`)
const commands = await importDirectory('./dist/commands')
logger.info(`Successfully loaded ${commands} ${botName} commands!`)

logger.info(`Loading ${botName} events...`)
const events = await importDirectory('./dist/events')
logger.info(`Successfully loaded ${events} ${botName} events!`)

await bot.start()

await createBanTable()
await createMessageCacheTable()

await runEachHour(() => unbanExpiredBans())
await runEachBeginningOfDay(() => removeExpiredMessages())