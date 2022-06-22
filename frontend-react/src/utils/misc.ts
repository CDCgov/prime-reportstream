import { showError } from "../components/AlertNotifications";

/**
 * splitOn('foo', 1);
 * // ["f", "oo"]
 *
 * splitOn([1, 2, 3, 4], 2);
 * // [[1, 2], [3, 4]]
 *
 * splitOn('fooBAr', 1, 4);
 * //  ["f", "ooB", "Ar"]
 */
export const splitOn: {
    <T = string>(s: T, ...i: number[]): T[];
    <T extends any[]>(s: T, ...i: number[]): T[];
} = <T>(slicable: string | T[], ...indices: number[]) =>
    [0, ...indices].map((n, i, m) => slicable.slice(n, m[i + 1]));

/**
 *
 * @param jsonTextValue string to be json.parsed
 * @param elemLabel used when displaying error in case there are multiple textarea's on the page
 * @param textInputRef used to select the range of text where the error happened.
 * @return false if fails to parse or the object of the successfully parsed json
 */
export const checkTextAreaJson = (
    jsonTextValue: string,
    elemLabel: string,
    textInputRef: React.RefObject<HTMLTextAreaElement>
): false | Object => {
    try {
        return JSON.parse(jsonTextValue);
    } catch (err: any) {
        // message like `'Unexpected token _ in JSON at position 164'`
        // or           `Unexpected end of JSON input`
        const errMsg = err?.message || "unknown error";
        showError(`Element "${elemLabel}" generated an error "${errMsg}"`);

        // now we parse out the position and try to select it for them.
        // NOTE: if "at position N" string not found, then assume mistake is at the end
        let errStartOffset = jsonTextValue.length;
        const findPositionMatch = errMsg?.matchAll(/position (\d+)/gi)?.next();
        if (findPositionMatch?.value?.length === 2) {
            const offset = parseInt(findPositionMatch.value[1] || -1);
            if (!isNaN(offset) && offset !== -1) {
                errStartOffset = offset;
            }
        }

        // now select the problem area inside the TextArea
        const errEndOffset = Math.min(errStartOffset + 4, jsonTextValue.length); // don't let go past len
        errStartOffset = Math.max(errStartOffset - 4, 0); // don't let go negative
        textInputRef?.current?.focus();
        textInputRef?.current?.setSelectionRange(errStartOffset, errEndOffset);
        return false;
    }
};

/**
 * returns the error detail usually found in the "error" field of the JSON returned
 * otherwise, just return the general exception detail
 */
export async function getErrorDetailFromResponse(e: any) {
    let errorResponse = await e?.response?.json();
    return errorResponse && errorResponse.error
        ? errorResponse.error
        : e.toString();
}

export enum VersionWarningType {
    POPUP = "popup",
    FULL = "full",
}

/**
 * returns a customized message in the case of trying to edit the wrong version of a setting
 * @param warningType either POPUP (for the toast notification) or FULL (for the red text on the compare modal itself)
 * @param settings the resource object from which to use information helpful to the user
 */
export function getVersionWarning(
    warningType: VersionWarningType,
    settings: any = null
): string {
    switch (warningType) {
        case VersionWarningType.POPUP:
            return `WARNING! A newer version of this setting now exists in the database'`;
        case VersionWarningType.FULL:
            return `WARNING! A change has been made to the setting you're trying to update by 
                    '${settings?.meta?.createdBy}'. Please coordinate with that user and return to update the setting 
                    again, if needed`;
    }

    return "";
}

export function formatDate(date: string): string {
    try {
        // 'Thu, 3/31/2022, 4:50 AM'
        return new Intl.DateTimeFormat("en-US", {
            weekday: "short",
            year: "numeric",
            month: "numeric",
            day: "numeric",
            hour: "numeric",
            minute: "numeric",
        }).format(new Date(date));
    } catch (err: any) {
        console.error(err);
        return date;
    }
}

/*
  for strings in machine readable form:
    * camel cased
    * inconsistent caps
    * whitespace deliminted by - or _

  translate into normal human readable strings with all words capitalized
*/
export const toHumanReadable = (machineString: string): string => {
    const delimitersToSpaces = machineString.replace(/[_-]/g, " ");
    const camelcaseToSpaces = delimitersToSpaces.replace(/([A-Z])/g, " $1");
    const fixCaps = camelcaseToSpaces.replace(
        /(?:\s|^)(\w)/g,
        (_match: string, capture: string) => ` ${capture.toUpperCase()}`
    );
    return fixCaps.trim();
};
