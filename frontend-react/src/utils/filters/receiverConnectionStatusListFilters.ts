import { RSReceiverStatus } from "../../hooks/api/UseReceiversConnectionStatus/UseReceiversConnectionStatus";

/**
 * Used by filter edit box ui to show only matched elements.
 * Allows some data to be excluded or cleaned up
 * @param search {string}
 */
export function filterOnName(obj: RSReceiverStatus, search: string | null): boolean {
    if (!search) {
        return true; // no search returns EVERYTHING
    }
    // combine all elements to be searched.
    return `${obj.organizationName} ${obj.receiverName}`.toLowerCase().includes(`${search.toLowerCase()}`);
}

export function filterOnCheckResultStr(obj: RSReceiverStatus, search: string | null): boolean {
    if (!search) {
        return true; // no search returns EVERYTHING
    }
    return `${obj.connectionCheckResult}`.toLowerCase().includes(`${search.toLowerCase()}`);
}
