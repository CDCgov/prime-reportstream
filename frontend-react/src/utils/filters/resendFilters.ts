import { RSResend } from "../../hooks/api/UseResends/UseResends";

export function filterMatch(obj: RSResend, search: string | null): boolean {
    if (!search) {
        return true; // no search returns EVERYTHING
    }
    // combine all elements to be searched.
    return `${obj.actionParams} ${obj.actionResponse} ${obj.actionResult}`
        .toLowerCase()
        .includes(`${search.toLowerCase()}`);
}
