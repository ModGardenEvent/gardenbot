import 'dotenv/config'

import { configs } from './config.js'
import { bot, logger } from './bot.js'
import { commands } from './util/commands.js'
import importDirectory from './util/loader.js'
import { botName } from './constants.js'

logger.info(`Upserting ${botName} commands!`)
await importDirectory('./dist/commands')
await updateAppCommands()
logger.info(`Successfully upserted ${commands.size} ${botName} commands!`)
await bot.shutdown()
process.exit(0)

async function updateAppCommands() : Promise<void> {
    await bot.helpers.upsertGuildApplicationCommands(configs.guildId, commands.map((value) => value.command))
}