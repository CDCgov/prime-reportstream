import { ReactElement, useCallback, useEffect, useState } from "react";
import DOMPurify from "dompurify";
import { ScrollSync, ScrollSyncPane } from "react-scroll-sync";
import { Checkbox } from "@trussworks/react-uswds";

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
    const [syncEnabled, setSyncEnabled] = useState(true);

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
        <>
            <ScrollSync enabled={syncEnabled}>
                <div className="rs-editable-compare-container differ-marks">
                    <div className="rs-editable-stacked-container">
                        <ScrollSyncPane>
                            <div
                                className="rs-static-compare-base rs-editable-compare-static"
                                data-testid={"left-compare-text"}
                                dangerouslySetInnerHTML={{
                                    __html: DOMPurify.sanitize(
                                        leftHighlightHtml
                                    ),
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
                                    __html: DOMPurify.sanitize(
                                        rightHighlightHtml
                                    ),
                                }}
                            />
                        </ScrollSyncPane>
                    </div>
                </div>
            </ScrollSync>
            <div className={"padding-bottom-2"}>
                <Checkbox
                    id={"scroll-sync-checkbox"}
                    name={"scroll-sync-checkbox"}
                    defaultChecked={syncEnabled}
                    data-testid={"scroll-sync-checkbox"}
                    label="Syncronize scrolling"
                    onChange={(e) => setSyncEnabled(e?.target?.checked)}
                />
            </div>
        </>
    );
};
