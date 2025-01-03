export const oneHour = 3600000
export const twentyFourHours = 86400000

export function closestHour(input: number) : number {
    return Math.floor(input - input % oneHour)
}

export function closestStartOfDay(input: number) : number {
    return Math.floor(input - input % twentyFourHours)
}

export async function runInIntervals(func: () => Promise<void>, ms: number) {
    setTimeout(async function () {
        func();
        setInterval(func, twentyFourHours);
    }, ms)
}

export async function runEachHour(func: () => Promise<void>) {
    const now = new Date();
    const time = new Date();

    time.setHours(now.getHours() + 1)
    time.setMinutes(0, 0, 0)

    const etaMs = time.getTime() - now.getTime();

    setTimeout(async function () {
        func();
        setInterval(func, oneHour);
    }, etaMs)
}

export async function runEachBeginningOfDay(func: () => Promise<void>) {
    const now = new Date();
    let zeroUtcHour = -Math.floor(now.getTimezoneOffset() / 60);
    let zeroUtcMinute = -Math.floor(now.getTimezoneOffset() % 60);
    if (zeroUtcHour < 0)
        zeroUtcHour += 24
    if (zeroUtcMinute < 0)
        zeroUtcMinute += 60

    let etaMs = new Date(now.getFullYear(), now.getMonth(), now.getDate(), zeroUtcHour, zeroUtcMinute).getTime() - now.getTime()
    if (etaMs < 0) {
        etaMs += twentyFourHours;
    }
    setTimeout(async function () {
        func();
        setInterval(func, twentyFourHours);
    }, etaMs)
}