import { ReactElement, useCallback, useEffect, useState } from "react";
import DOMPurify from "dompurify";
import { ScrollSync, ScrollSyncPane } from "react-scroll-sync";

import { Diff, SES_TYPE } from "../utils/diff";
import { splitOn } from "../utils/misc";

interface StaticCompareProps {
    rightText: string;
    leftText: string;
}

/**
 * local function that puts `<mark></mark>` around text
 * @param s1 input string
 * @param offset start of text to have <mark>
 * @param length length of text to have <mark>
 * @return updated string
 */
const insertHighlight = (
    s1: string,
    offset: number,
    length: number
): string => {
    if (s1 === "" || length === 0) {
        return "";
    }
    // we want to insert a <span></span> around text.
    const threeParts = splitOn(s1, offset, offset + length);
    if (threeParts.length !== 3) {
        console.error("split failed");
        return s1;
    }

    return `${threeParts[0]}<mark>${threeParts[1]}</mark>${threeParts[2]}`;
};

export const StaticCompare = (props: StaticCompareProps): ReactElement => {
    const [leftHighlightHtml, setLeftHighlightHtml] = useState("");
    const [rightHighlightHtml, setRightHighlightHtml] = useState("");

    const refreshHighlights = useCallback(
        (rightText: string, leftText: string) => {
            if (rightText.length === 0 || leftText.length === 0) {
                return;
            }

            const differ = Diff(rightText, leftText);
            differ.compose();
            const sesArray = differ.getses();

            // because we're modifying text, it will change offsets UNLESS we go backwards.
            // the later items in the patches array are sequential
            let patchedLeftStr = rightText;
            let patchedRightStr = leftText;

            for (let i = sesArray.length - 1; i >= 0; --i) {
                const eachSes = sesArray[i];
                if (eachSes.sestype === SES_TYPE.DELETE) {
                    patchedLeftStr = insertHighlight(
                        patchedLeftStr,
                        eachSes.index - 1,
                        eachSes.len
                    );
                } else if (eachSes.sestype === SES_TYPE.ADD) {
                    patchedRightStr = insertHighlight(
                        patchedRightStr,
                        eachSes.index - 1,
                        eachSes.len
                    );
                } // ignore SES_TYPE.COMMON
            }

            // now stick it back into the edit boxes.
            if (patchedLeftStr !== leftHighlightHtml) {
                setLeftHighlightHtml(patchedLeftStr);
            }

            // we only change the hightlighting on the BACKGROUND div so we don't mess up typing/cursor
            if (patchedRightStr !== rightHighlightHtml) {
                setRightHighlightHtml(patchedRightStr);
            }
        },
        [leftHighlightHtml, rightHighlightHtml]
    );

    useEffect(() => {
        if (props.leftText?.length > 0 && props.rightText?.length > 0) {
            // initialization only
            refreshHighlights(props.leftText, props.rightText);
        }
    }, [props.leftText, props.rightText, refreshHighlights]);

    return (
        <ScrollSync>
            <div className="rs-editable-compare-container">
                {/*Text has two components. The text on top and the highlight under it.*/}
                {/*scrolling is synced so they line up*/}

                <div className="rs-editable-stacked-container">
                    <ScrollSyncPane>
                        <div
                            className="rs-static-compare-base rs-editable-compare-static"
                            data-testid={"left-compare-text"}
                            contentEditable={false}
                            dangerouslySetInnerHTML={{
                                __html: DOMPurify.sanitize(props.leftText),
                            }}
                        />
                    </ScrollSyncPane>
                    <ScrollSyncPane>
                        <div
                            className="rs-static-compare-base rs-editable-compare-background"
                            data-testid={"left-compare-highlight"}
                            dangerouslySetInnerHTML={{
                                __html: DOMPurify.sanitize(leftHighlightHtml),
                            }}
                        />
                    </ScrollSyncPane>
                </div>

                {/*Text has two components. The text on top and the highlight under it.*/}
                {/*scrolling is synced so they line up*/}
                <div className="rs-editable-stacked-container">
                    <ScrollSyncPane>
                        <div
                            className="rs-static-compare-base rs-editable-compare-static"
                            data-testid={"right-compare-text"}
                            dangerouslySetInnerHTML={{
                                __html: DOMPurify.sanitize(props.rightText),
                            }}
                        />
                    </ScrollSyncPane>

                    <ScrollSyncPane>
                        <div
                            className="rs-static-compare-base rs-editable-compare-background"
                            data-testid={"right-compare-highlight"}
                            dangerouslySetInnerHTML={{
                                __html: `${DOMPurify.sanitize(
                                    rightHighlightHtml
                                )}<br/>`,
                            }}
                        />
                    </ScrollSyncPane>
                </div>
            </div>
        </ScrollSync>
    );
};
