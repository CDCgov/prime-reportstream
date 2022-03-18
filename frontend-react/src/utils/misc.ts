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
 * @param textValue string to be json.parsed
 * @param elemLabel used when displaying error in case there are multiple textarea's on the page
 * @param textInputRef used to select the range of text where the error happened.
 * @return false if fails to parse or the object of the successfully parsed json
 */
export const checkTextAreaJson = (
    textValue: string,
    elemLabel: string,
    textInputRef: React.RefObject<HTMLTextAreaElement>
): false | Object => {
    try {
        return JSON.parse(textValue);
    } catch (err: any) {
        // message like `'Unexpected token _ in JSON at position 164'`
        // or           `Unexpected end of JSON input`
        const errMsg = err?.message || "unknown error";
        showError(`Element "${elemLabel}" generated an error "${errMsg}"`);

        // now we parse out the position and try to select it for them.
        // NOTE: if position string not found, then assume mistake is at the end
        let errOffset = errMsg.length;
        const findPositionMatch = errMsg?.matchAll(/position (\d+)/gi)?.next();
        if (findPositionMatch?.value?.length === 2) {
            const offset = parseInt(findPositionMatch.value[1] || -1);
            if (!isNaN(offset) && offset !== -1) {
                errOffset = offset;
            }
        }

        // now select the problem area inside the TextArea
        if (errOffset > 4) {
            errOffset -= 4;
        }
        const end = Math.min(errOffset + 8, textValue.length);
        textInputRef?.current?.focus();
        textInputRef?.current?.setSelectionRange(errOffset, end);
        return false;
    }
};
