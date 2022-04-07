import {showError} from "../components/AlertNotifications";

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
export async function getErrorDetail(e: any) {
    let errorResponse = await e?.response?.json();
    return errorResponse && errorResponse.error
        ? errorResponse.error
        : e.toString();
}
