export interface TimestampObject {
    timestamp: string;
}

function sortByTimestamp(a: TimestampObject, b: TimestampObject): number {
    // format "2022-02-01T15:11:58.200754Z" means we can compare strings without converting to dates
    // since it's in descending time format (aka year, month, day, hour, min, sec)
    if (a.timestamp === b.timestamp) {
        return 0;
    }
    return a.timestamp > b.timestamp ? -1 : 1;
}

export default sortByTimestamp;
