import { readdir } from 'node:fs/promises'
import { logger } from '../bot.js'

export default async function importDirectory(folder: string): Promise<number> {
  const files = await readdir(folder, { recursive: true })

  let successes = 0

  for (const filename of files) {
    if (!filename.endsWith('.js')) continue

    // Using `file://` and `process.cwd()` to avoid weird issues with relative paths and/or Windows
    await import(`file://${process.cwd()}/${folder}/${filename}`).catch((x) => {
        logger.fatal(`Cannot import file (${folder}/${filename}) for reason: ${x}`)
        // Jank...
        --successes
    }).finally(() => 
        ++successes
    )
  }

  return successes
}