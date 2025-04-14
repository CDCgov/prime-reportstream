import { format, fromUnixTime, isValid, parseISO } from "date-fns";

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
export const generateDateTitles = (dateTimeString?: string, useNow?: boolean) => {
    if (!(dateTimeString || useNow)) {
        return { dateString: "N/A", timeString: "N/A" };
    }

    const date = useNow ? new Date() : parseISO(dateTimeString!);

    if (!isValid(date) && useNow !== true) {
        return { dateString: "N/A", timeString: "N/A" };
    }

    return {
        dateString: format(date, "d LLL yyyy"),
        timeString: format(date, "HH:mm"),
    };
};

export function isDateExpired(dateTimeString: string | number) {
    const now = new Date();

    const dateToCompare =
        typeof dateTimeString === "string"
            ? !/^\d+$/.test(dateTimeString)
                ? parseISO(dateTimeString)
                : fromUnixTime(parseInt(dateTimeString))
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

/*
 * format: "Mon, 7/25/2022"
 * WARNING: Intl.DateTimeFormat() can be slow if called in a loop!
 * Rewrote to just use Date to save cpu
 * */
export const dateShortFormat = (d: Date) => {
    const dayOfWeek = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"][d.getDay()] || "";
    return `${dayOfWeek}, ` + `${d.getMonth() + 1}/${d.getDate()}/${d.getFullYear()}`;
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

export type DatePair = [start: Date, end: Date];
