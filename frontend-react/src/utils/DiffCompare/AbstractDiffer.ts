import { splitOn } from "../misc";

export type DifferMarkupResult = {
    left: { normalized: string; markupText: string };
    right: { normalized: string; markupText: string };
};

/**
 * Adds a `<mark>` around text for start/end. Used by differs
 * @param text
 * @param start
 * @param end
 */
export const insertMark = (
    text: string,
    start: number,
    end: number
): string => {
    if (text === "" || end - start <= 0 || end >= text.length) {
        return text;
    }
    const threeParts = splitOn(text, start, end);
    if (threeParts.length !== 3) {
        return text;
    }
    return `${threeParts[0]}<mark>${threeParts[1]}</mark>${threeParts[2]}`;
};
