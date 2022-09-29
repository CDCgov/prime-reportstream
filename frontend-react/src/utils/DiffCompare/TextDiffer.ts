/**
 *  Moved code from the EditableCompare into a common file. It mirrors the calls to JsonDiffer
 */

import { Diff, SES_TYPE } from "./diff";
import { DifferMarkupResult, insertMark } from "./AbstractDiffer";

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
    rightText: string
): DifferMarkupResult => {
    const differ = Diff(leftText, rightText);
    differ.compose();
    const sesses = differ.getses();

    // Left items are "Deleted" in the Sess world
    const leftMarkupText = sesses.reduce(
        (acc, eachDiff) =>
            eachDiff.sestype !== SES_TYPE.DELETE
                ? acc
                : insertMark(
                      acc,
                      eachDiff.index - 1,
                      eachDiff.index - 1 + eachDiff.len
                  ),
        leftText
    );

    // Right items are "Added" in the Sess world
    const rightMarkupText = sesses.reduce(
        (acc, eachDiff) =>
            eachDiff.sestype !== SES_TYPE.ADD
                ? acc
                : insertMark(
                      acc,
                      eachDiff.index - 1,
                      eachDiff.index - 1 + eachDiff.len
                  ),
        rightText
    );

    /** we're given the opportunity to normalize the text*/
    return {
        left: { normalized: leftText, markupText: leftMarkupText },
        right: { normalized: rightText, markupText: rightMarkupText },
    };
};
