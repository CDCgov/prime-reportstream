import { splitOn } from "../misc";

export type DifferMarkupResult = {
    left: { normalized: string; markupText: string };
    right: { normalized: string; markupText: string };
};

/**
 * Adds a `<mark>` around text for start/end. Used by differs
 * @param text input string
 * @param offset start of text to have <mark>
 * @param length length of text to have <mark>
 * @return updated string
 */
export const insertMark = (
    text: string,
    offset: number,
    length: number
): string => {
    if (
        text.length === 0 ||
        offset < 0 ||
        offset > text.length ||
        length === 0 ||
        length >= text.length
    ) {
        return text;
    }

    // we want to insert a <mark></mark> around text.
    const threeParts = splitOn(text, offset, offset + length);
    if (threeParts.length !== 3) {
        console.error("split failed");
        return text;
    }

    return `${threeParts[0]}<mark>${threeParts[1]}</mark>${threeParts[2]}`;
};
