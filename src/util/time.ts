const twentyFourHours = 86400000

export function closestStartOfDay(input: number) : number {
    return Math.floor(input - input % twentyFourHours)
}

export async function runInIntervals(func: () => Promise<void>, ms: number) {
    setTimeout(async function () {
        func();
        setInterval(func, twentyFourHours);
    }, ms)
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