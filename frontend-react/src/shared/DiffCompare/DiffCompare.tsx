import DOMPurify from "dompurify";
import { ScrollSync, ScrollSyncPane } from "react-scroll-sync";

import "./JsonCompare.scss";
import useJsonDiff from "../../hooks/UseJsonDiff/UseJsonDiff";

interface DiffCompareBaseProps {
    a: string;
    b: string;
    aDiffHtml: string;
    bDiffHtml: string;
}

/**
 *
 * Warning: DO NOT assign a `key` to the div inside `<ScrollSyncPane>` it will break it.
 *
 * NOTES:
 *
 * - The diff is written for unstructured text, it's probably better to replace with a
 *      json differ and pass json into the function instead of text. Rewrite to understand json!
 *
 * - Doing highlights in js is a well defined problem. But go check out this AMAZING codepen
 *      https://codepen.io/lonekorean/pen/gaLEMR  (make sure to click on the Toogle Prespective button)
 *
 * **/

export function DiffCompareBase({
    a,
    aDiffHtml,
    b,
    bDiffHtml,
}: DiffCompareBaseProps) {
    return (
        <ScrollSync>
            <div className="rs-editable-compare-container differ-marks">
                <div className="rs-editable-stacked-container">
                    <ScrollSyncPane>
                        <pre className="rs-editable-compare-base rs-editable-compare-static">
                            {a}
                        </pre>
                    </ScrollSyncPane>

                    <ScrollSyncPane>
                        <pre
                            className="rs-editable-compare-base rs-editable-compare-background"
                            dangerouslySetInnerHTML={{
                                __html: DOMPurify.sanitize(aDiffHtml),
                            }}
                        />
                    </ScrollSyncPane>
                </div>
                <div className="rs-editable-stacked-container">
                    <ScrollSyncPane>
                        <pre className="rs-editable-compare-base rs-editable-compare-edit">
                            {b}
                        </pre>
                    </ScrollSyncPane>

                    <ScrollSyncPane>
                        <pre
                            className="rs-editable-compare-base rs-editable-compare-background"
                            dangerouslySetInnerHTML={{
                                __html: DOMPurify.sanitize(bDiffHtml),
                            }}
                        />
                    </ScrollSyncPane>
                </div>
            </div>
        </ScrollSync>
    );
}

export interface DiffCompareProps {
    a: object;
    b: object;
}

export function DiffCompare({ a, b }: DiffCompareProps) {
    const { left, right } = useJsonDiff(a, b);
    return (
        <DiffCompareBase
            a={left.normalized}
            b={right.normalized}
            aDiffHtml={left.markupText}
            bDiffHtml={right.markupText}
        />
    );
}
