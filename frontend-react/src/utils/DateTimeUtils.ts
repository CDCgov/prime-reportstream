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
    dateTimeString: string
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
    format: string
) => {
    const timestampDate = new Date(timestamp);
    // TODO: refactor to remove dependency on moment
    return moment(timestampDate).format(format);
};

export const timeZoneAbbreviated = () => {
    return Intl.DateTimeFormat().resolvedOptions().timeZone;
};
