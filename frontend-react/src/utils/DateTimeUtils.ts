import { format, fromUnixTime, isValid, parseISO } from "date-fns";

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
