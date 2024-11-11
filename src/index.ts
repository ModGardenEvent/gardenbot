import 'dotenv/config'

import importDirectory from './util/loader.js'
import { bot, logger } from './bot.js'
import { createBanDb, unbanExpiredBans } from './util/banDatabase.js'
import { runEachBeginningOfDay } from './util/time.js'
import { botName } from './constants.js'

logger.info(`Starting ${botName}...`)

logger.info(`Loading ${botName} commands...`)
const commands = await importDirectory('./dist/commands')
logger.info(`Successfully loaded ${commands} ${botName} commands!`)

logger.info(`Loading ${botName} events...`)
const events = await importDirectory('./dist/events')
logger.info(`Successfully loaded ${events} ${botName} events!`)

await bot.start()

await createBanDb()
await runEachBeginningOfDay(() => unbanExpiredBans())