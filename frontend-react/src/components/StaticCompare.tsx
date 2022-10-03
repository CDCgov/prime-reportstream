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
        (leftText: string, rightText: string, jsonDiffMode: boolean) => {
            if (leftText.length === 0 || rightText.length === 0) {
                // just clear the hightlighting for both sides and don't bother comparing
                setLeftHighlightHtml(leftText);
                setRightHighlightHtml(rightText);
                return;
            }
            const result = jsonDiffMode
                ? jsonDifferMarkup(JSON.parse(leftText), JSON.parse(rightText))
                : textDifferMarkup(leftText, rightText);

            setLeftHighlightHtml(result.left.markupText);
            setRightHighlightHtml(result.right.markupText);
        },
        []
    );

    useEffect(() => {
        refreshHighlights(props.leftText, props.rightText, props.jsonDiffMode);
    }, [
        props.leftText,
        props.rightText,
        props.jsonDiffMode,
        refreshHighlights,
    ]);

    return (
        <ScrollSync>
            <div className="rs-editable-compare-container differ-marks">
                <div className="rs-editable-stacked-container">
                    <ScrollSyncPane>
                        <div
                            className="rs-static-compare-base rs-editable-compare-static"
                            data-testid={"left-compare-text"}
                            dangerouslySetInnerHTML={{
                                __html: DOMPurify.sanitize(leftHighlightHtml),
                            }}
                        />
                    </ScrollSyncPane>
                </div>
                <div className="rs-editable-stacked-container">
                    <ScrollSyncPane>
                        <div
                            className="rs-static-compare-base rs-editable-compare-static"
                            data-testid={"right-compare-text"}
                            dangerouslySetInnerHTML={{
                                __html: DOMPurify.sanitize(rightHighlightHtml),
                            }}
                        />
                    </ScrollSyncPane>
                </div>
            </div>
        </ScrollSync>
    );
};
