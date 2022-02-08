export type SubmissionDate = {
    dateString: string;
    timeString: string;
};

/* 
    This function serves as a cleaner (read: more contained) way of parsing out
    necessary date and time string formats for this page.

    @param dateTimeString - the value representing when a report was sent, returned
    by the API  
    
    @returns SubmissionDate
    dateString format: 1 Jan 2022
    timeString format: 3:00 PM
*/
export const generateSubmissionDate = (
    dateTimeString: string
): SubmissionDate => {
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

    /* Parse time into parts */
    const minutes = dateTimeISO.getMinutes();
    let hours = dateTimeISO.getHours();
    let meridian = "am";

    /* 12-hour and meridian conversion */
    if (hours > 12) {
        /* Afternoon/evening */
        hours -= 12;
        meridian = "pm";
    } else if (hours === 0) {
        /* Midnight */
        hours = 12;
        meridian = "am";
    }

    /* Create strings from parsed values */
    const time = `${hours}:${minutes} ${meridian.toUpperCase()}`;
    const date = `${dateTimeISO.getDate()} ${
        monthNames[dateTimeISO.getMonth()]
    } ${dateTimeISO.getFullYear()}`;

    return {
        dateString: date,
        timeString: time,
    };
};
