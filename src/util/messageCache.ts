import { Message } from "@discordeno/bot"

class CacheMap extends Map<bigint, Message> {
    timeouts = new Map<bigint, NodeJS.Timeout>() 

    override set(key: bigint, value: Message): this {
        const val = super.set(key, value)
        this.timeouts.set(key, setTimeout(() => {
            this.delete(key)
        }, 1800000))
        return val;
    }

    override delete(key: bigint): boolean {
        const val = super.delete(key)
        if (this.timeouts.has(key)) {
            clearTimeout(this.timeouts.get(key))
            this.timeouts.delete(key)
        }
        return val;
    }
}

export const messageCache = new CacheMap()