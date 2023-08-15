import moment from "moment";

export type DateTimeData = {
    dateString: string;
    timeString: string;
};

/*
    This function serves as a cleaner (read: more contained) way of parsing out
    necessary date and time string formats for the Submissions details page.

    @param dateTimeString - the value representing when a report was sent, returned
    by the API

    @returns DateTimeData | null
    dateString format: 1 Jan 2022
    timeString format: 16:30
*/
export const generateDateTitles = (
    dateTimeString: string,
): DateTimeData | null => {
    const dateRegex = /\d{1,2} [a-z,A-Z]{3} \d{4}/;
    const timeRegex = /\d{1,2}:\d{2}/;

    const date = new Date(dateTimeString);
    const monthString = parseMonth(date.getMonth());

    const dateString = `${date.getDate()} ${monthString} ${date.getFullYear()}`;
    const timeString = `${date.getHours()}:${getPaddedMinutes(date)}`;

    if (!dateString.match(dateRegex) || !timeString.match(timeRegex))
        return null;

    return {
        dateString: dateString,
        timeString: timeString,
    };
};

const getPaddedMinutes = (date: Date) => {
    const minutes = date.getMinutes().toString();
    return minutes.length > 1 ? minutes : `0${minutes}`;
};

const parseMonth = (numericMonth: number) => {
    const monthNames = [
        "Jan",
        "Feb",
        "Mar",
        "Apr",
        "May",
        "Jun",
        "Jul",
        "Aug",
        "Sep",
        "Oct",
        "Nov",
        "Dec",
    ];
    return monthNames[numericMonth];
};

export const formattedDateFromTimestamp = (
    timestamp: string,
    format: string,
) => {
    const timestampDate = new Date(timestamp);
    // TODO: refactor to remove dependency on moment
    return moment(timestampDate).format(format);
};

export const timeZoneAbbreviated = () => {
    return Intl.DateTimeFormat().resolvedOptions().timeZone;
};

export function isDateExpired(dateTimeString: string | number) {
    const now = moment.utc().local();
    const dateToCompare = moment.utc(dateTimeString).local();
    return dateToCompare < now;
}

export function transformDate(s: string) {
    return new Date(s).toLocaleString();
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
