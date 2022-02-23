export type SubmissionDate = {
    dateString: string;
    timeString: string;
};

/* 
    This function serves as a cleaner (read: more contained) way of parsing out
    necessary date and time string formats for this page.

    @param dateTimeString - the value representing when a report was sent, returned
    by the API  
    
    @returns SubmissionDate | null
    dateString format: 1 Jan 2022
    timeString format: 3:00 PM
*/
export const generateSubmissionDate = (
    dateTimeString: string
): SubmissionDate | null => {
    const inputRegex = /\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}.\d{0,5}Z/;
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

    /* Converts to local timezone as a Date object */
    const dateTimeISO = new Date(dateTimeString);

    /* Catch bad input */
    if (!dateTimeString.match(inputRegex)) return null;

    /* Parse time into parts */
    const minutes: number = dateTimeISO.getMinutes();
    let hours: number = dateTimeISO.getHours();
    let meridian: string = "am";

    /* 12-hour and meridian conversion */
    if (hours > 12) {
        /* Afternoon/evening */
        hours -= 12;
        meridian = "pm";
    } else if (hours === 0) {
        /* Midnight */
        hours = 12;
    }

    /* Create strings from parsed values */
    const time: string = `${hours}:${minutes} ${meridian.toUpperCase()}`;
    const date: string = `${dateTimeISO.getDate()} ${
        monthNames[dateTimeISO.getMonth()]
    } ${dateTimeISO.getFullYear()}`;

    return {
        dateString: date,
        timeString: time,
    };
};
