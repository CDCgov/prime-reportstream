/**
 *  Moved code from the EditableCompare into a common file. It mirrors the calls to JsonDiffer
 */

import { splitOn } from "../misc";

import { Diff, SES_TYPE } from "./diff";
import { DifferMarkupResult } from "./AbstractDiffer";

/**
 * TODO: this approach to inserting marks is NOT as robust as insertMarks() in JsonDiffer. Refactor.
 * Adds a `<mark>` around text for start/end. Used by differs
 * @param text input string
 * @param offset start of text to have <mark>
 * @param length length of text to have <mark>
 * @return updated string
 */
export const insertHighlight = (
    text: string,
    offset: number,
    length: number,
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
        console.warn("split failed");
        return text;
    }

    return `${threeParts[0]}<mark>${threeParts[1]}</mark>${threeParts[2]}`;
};

/**
 * The original json differ was just text. It now is smart about the structured json data.
 * This text differ is still around because it might be useful for formats that are NOT json
 * e.g. `.yml`, `FHIR`, `HL7`
 * In theory, those could each get smarter parses/differs eventually.
 * @param leftText
 * @param rightText
 */
export const textDifferMarkup = (
    leftText: string,
    rightText: string,
): DifferMarkupResult => {
    const differ = Diff(leftText, rightText);
    differ.compose();
    const sesses = differ.getses();

    // reverse the sesses to work through them backwards to insert marks
    sesses.sort((a, b) => b.index - a.index);

    // Left items are "Deleted" in the Sess world
    const leftMarkupText = sesses.reduce(
        (acc, eachDiff) =>
            eachDiff.sestype !== SES_TYPE.DELETE
                ? acc
                : insertHighlight(acc, eachDiff.index - 1, eachDiff.len),
        leftText,
    );

    // Right items are "Added" in the Sess world
    const rightMarkupText = sesses.reduce(
        (acc, eachDiff) =>
            eachDiff.sestype !== SES_TYPE.ADD
                ? acc
                : insertHighlight(acc, eachDiff.index - 1, eachDiff.len),
        rightText,
    );

    /** we're given the opportunity to normalize the text*/
    return {
        left: { normalized: leftText, markupText: leftMarkupText },
        right: { normalized: rightText, markupText: rightMarkupText },
    };
};
