import { ReactElement, useCallback, useEffect, useState } from "react";
import DOMPurify from "dompurify";
import { ScrollSync, ScrollSyncPane } from "react-scroll-sync";

import { Diff, SES_TYPE } from "../utils/diff";
import { splitOn } from "../utils/misc";

interface StaticCompareProps {
    rightText: string;
    leftText: string;
}

const insertHighlight = (
    s1: string,
    offset: number,
    length: number
): string => {
    if (s1 === "" || length === 0) {
        return "";
    }
    // we want to insert a <span></span> around text.
    const three_parts = splitOn(s1, offset, offset + length);
    if (three_parts.length !== 3) {
        console.error("split failed");
        return s1;
    }

    return `${three_parts[0]}<mark>${three_parts[1]}</mark>${three_parts[2]}`;
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
            const sesses = differ.getses();

            // because we're modifying text, it will change offsets UNLESS we go backwards.
            // the later items in the patches array are sequential
            let patchedLeftStr = rightText;
            let patchedRightStr = leftText;

            for (let ii = sesses.length - 1; ii >= 0; --ii) {
                const eachses = sesses[ii];
                if (eachses.sestype === SES_TYPE.DELETE) {
                    patchedLeftStr = insertHighlight(
                        patchedLeftStr,
                        eachses.index - 1,
                        eachses.len
                    );
                } else if (eachses.sestype === SES_TYPE.ADD) {
                    patchedRightStr = insertHighlight(
                        patchedRightStr,
                        eachses.index - 1,
                        eachses.len
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
    }, [props, refreshHighlights]);

    return (
        <ScrollSync>
            <div className="rs-editable-compare-container">
                {/*Text has two components. The text on top and the highlight under it.*/}
                {/*scrolling is synced so they line up*/}

                <div className="rs-editable-stacked-container">
                    <ScrollSyncPane>
                        <div
                            className="rs-static-compare-base rs-editable-compare-static"
                            contentEditable={false}
                            dangerouslySetInnerHTML={{
                                __html: DOMPurify.sanitize(props.leftText),
                            }}
                        />
                    </ScrollSyncPane>
                    <ScrollSyncPane>
                        <div
                            className="rs-static-compare-base rs-editable-compare-background"
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
                            dangerouslySetInnerHTML={{
                                __html: DOMPurify.sanitize(props.rightText),
                            }}
                        />
                    </ScrollSyncPane>

                    <ScrollSyncPane>
                        <div
                            className="rs-static-compare-base rs-editable-compare-background"
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
