import { ReactElement, useCallback, useEffect, useState } from "react";
import DOMPurify from "dompurify";
import { ScrollSync, ScrollSyncPane } from "react-scroll-sync";

import { jsonDifferMarkup } from "../utils/DiffCompare/JsonDiffer";
import { textDifferMarkup } from "../utils/DiffCompare/TextDiffer";

interface StaticCompareProps {
    rightText: string;
    leftText: string;
    jsonDiffMode: boolean; // true is json aware compare, false is a text compare
}

export const StaticCompare = (props: StaticCompareProps): ReactElement => {
    const [leftHighlightHtml, setLeftHighlightHtml] = useState("");
    const [rightHighlightHtml, setRightHighlightHtml] = useState("");

    const refreshHighlights = useCallback(
        (rightText: string, leftText: string) => {
            if (rightText.length === 0 || leftText.length === 0) {
                return;
            }

            const result = props.jsonDiffMode
                ? jsonDifferMarkup(props.leftText, props.rightText)
                : textDifferMarkup(props.leftText, props.rightText);

            debugger;
            // now stick it back into the edit boxes.
            if (result.left.markupText !== leftHighlightHtml) {
                setLeftHighlightHtml(result.left.markupText);
            }

            // we only change the hightlighting on the BACKGROUND div so we don't mess up typing/cursor
            if (result.right.markupText !== rightHighlightHtml) {
                setRightHighlightHtml(result.right.markupText);
            }
        },
        [
            leftHighlightHtml,
            rightHighlightHtml,
            props.leftText,
            props.rightText,
            props.jsonDiffMode,
        ]
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
