import { addMinutes, endOfDay, format, fromUnixTime, isValid, parseISO, startOfDay, subDays, subMilliseconds } from "date-fns";

export const DAY_BACK_DEFAULT = 3 - 1; // N days (-1 because we add a day later for ranges)

export interface DateTimeData {
    dateString: string;
    timeString: string;
}

/*
    This function serves as a cleaner (read: more contained) way of parsing out
    necessary date and time string formats for the Submissions details page.

    @param dateTimeString - the value representing when a report was sent, returned
    by the API

    @returns DateTimeData | null
    dateString format: 1 Jan 2022
    timeString format: 16:30
*/
export const generateDateTitles = (dateTimeString?: string) => {
    if (!dateTimeString) {
        return { dateString: "N/A", timeString: "N/A" };
    }

    const date = parseISO(dateTimeString);

    if (!isValid(date)) {
        return { dateString: "N/A", timeString: "N/A" };
    }

    return {
        dateString: format(date, "d LLL yyyy"),
        timeString: format(date, "HH:mm"),
    };
};

export function isDateExpired(dateTimeString: string | number) {
    // eslint-disable-next-line import/no-named-as-default-member
    const now = new Date();
    // eslint-disable-next-line import/no-named-as-default-member
    const dateToCompare =
        typeof dateTimeString === "string"
            ? parseISO(dateTimeString)
            : fromUnixTime(dateTimeString);
    return dateToCompare < now;
}

// Will return date with format: 2/9/2023, 02:24 PM
export function formatDateWithoutSeconds(d: string) {
    const date = d === "" ? new Date() : new Date(d);
    const newDate = new Date(date).toLocaleString([], {
        year: "numeric",
        month: "numeric",
        day: "numeric",
        hour: "2-digit",
        minute: "2-digit",
    });
    return newDate.replace(/,/, "");
}

/**
 *  simple iterator to make other code more readable.
 *  Usage:
 *    for (let eachTimeSlot in (new TimeSlots([dateStart, dateEnd], 2)) { }
 */
export interface IterateTimeSlots {
    [Symbol.iterator]: () => Iterator<DatePair>;
}

export class TimeSlots implements Iterable<DatePair> {
    private current: Date;
    private readonly end: Date;
    private readonly skipMinutes: number;

    constructor(range: DatePair, skipMinutes = 2 * 60) {
        this.current = range[0];
        this.end = range[1];
        this.skipMinutes = skipMinutes;
    }

    *[Symbol.iterator]() {
        do {
            const next = addMinutes(this.current, this.skipMinutes)
            const end = subMilliseconds(next, 1);
            yield [this.current, end] as DatePair;
            this.current = next;
        } while (this.current < this.end);
    }

    *map(mapper: (pair: DatePair) => unknown){
        for(const next of this) {
            yield mapper(next);
        }
    }

    *drop(num: number) {
        let remaining = num - 1;
        const iter = this[Symbol.iterator]()

        do {
            const next = iter.next()
            if(next.done) return;
        } while(remaining-- > 0)

        while(1) {
            const {done, value} = iter.next();
            if(done) return;
            yield value;
        }
    }

    toArray() {
        return Array.from(this)
    }
}

// mostly for readably
export type DatePair = [Date, Date];

export function dateIsInRange(d: Date, range: DatePair): boolean {
    return d >= range[0] && d < range[1];
}

/*
 * format: "Mon, 7/25/2022"
 * WARNING: Intl.DateTimeFormat() can be slow if called in a loop!
 * Rewrote to just use Date to save cpu
 * */
export const dateShortFormat = (d: Date) => {
    const dayOfWeek =
        ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"][d.getDay()] || "";
    return (
        `${dayOfWeek}, ` +
        `${d.getMonth() + 1}/${d.getDate()}/${d.getFullYear()}`
    );
};

/**
 * Result string is like "12h 34m 05.678s"
 * No duration returns ""
 * @param dateNewer Date
 * @param dateOlder Date
 */
export const durationFormatShort = (dateNewer: Date, dateOlder: Date): string => {
    const msDiff = dateNewer.getTime() - dateOlder.getTime();
    const hrs = Math.floor(msDiff / (60 * 60 * 1000)).toString();
    const mins = Math.floor((msDiff / (60 * 1000)) % 60).toString();
    // 0.1200001 -> '0.12`
    const secs = parseFloat(((msDiff / 1000) % 60).toFixed(3)).toString();

    const parts = [];
    if (hrs !== "0") {
        parts.push(`${hrs}h`);
    }
    if (parts.length || mins !== "0") {
        const minsPad = mins.length < 2 ? "0" + mins : mins;
        parts.push(`${minsPad}m`);
    }
    if (parts.length || secs !== "0") {
        const secsPad = secs.indexOf(".") < 2 ? "0" + secs : secs;
        parts.push(`${secsPad}s`);
    }
    return parts.join(" ");
};

/**
 * Declared outside of the function so React doesn't update constantly (also, time is fixed)
 * Usage: `yesterday()`
 * @param d {Date}
 * @return {string}
 */
export const startOfDayIso = (d: Date) => {
    return startOfDay(d).toISOString();
};

export const endOfDayIso = (d: Date) => {
    return endOfDay(d).toISOString();
};

export const initialStartDate = () => {
    return subDays(new Date(), DAY_BACK_DEFAULT);
};

export const initialEndDate = () => {
    return new Date();
};