import {
    forwardRef,
    ReactElement,
    Ref,
    useCallback,
    useEffect,
    useImperativeHandle,
    useRef,
    useState,
} from "react";
import DOMPurify from "dompurify";
import { ScrollSync, ScrollSyncPane } from "react-scroll-sync";

import { Diff, SES_TYPE } from "../utils/diff";
import { checkTextAreaJson, splitOn } from "../utils/misc";

// interface on Component that is callable
export type EditableCompareRef = {
    getEditedText: () => string;
    getOriginalText: () => string;
    refreshEditedText: (updatedjson: string) => void;
};

interface EditableCompareProps {
    original: string;
    modified: string;
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

export const EditableCompare = forwardRef(
    // allows for functions on components (useImperativeHandle)
    (
        props: EditableCompareProps,
        ref: Ref<EditableCompareRef>
    ): ReactElement => {
        // useRefs are used to access html elements directly (instead of document.getElementById)
        const staticDiffRef = useRef<HTMLDivElement>(null);
        const editDiffRef = useRef<HTMLTextAreaElement>(null);
        const editDiffBackgroundRef = useRef<HTMLDivElement>(null);

        const [textAreaContent, setTextAreaContent] = useState("");

        const [leftHandSideHighlightHtml, setLeftHandSideHighlightHtml] =
            useState("");
        const [rightHandSideHighlightHtml, setRightHandSideHighlightHtml] =
            useState("");

        useImperativeHandle(
            ref,
            () => ({
                getEditedText() {
                    return textAreaContent;
                },
                getOriginalText() {
                    return props.original;
                },
                // when showing/hiding json, force am update of the content
                refreshEditedText(updatedjson) {
                    setTextAreaContent(updatedjson);
                    onChangeHandler(updatedjson);
                },
            }),
            // onChangeHandler appears below, remove from deps
            // eslint-disable-next-line
            [textAreaContent]
        );

        const turnOffSpellCheckSwigglies = () => {
            if (editDiffRef?.current?.spellcheck) {
                editDiffRef.current.spellcheck = false;
            }
        };

        const refreshDiffCallback = useCallback(
            (originalText: string, modifiedText: string) => {
                if (originalText.length === 0 || modifiedText.length === 0) {
                    return;
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

                turnOffSpellCheckSwigglies();

                const differ = Diff(originalText, modifiedText);
                differ.compose();
                const sesses = differ.getses();

                // because we're modifying text, it will change offsets UNLESS we go backwards.
                // the later items in the patches array are sequential
                let patchedLeftStr = originalText;
                let patchedRightStr = modifiedText;

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
                if (patchedLeftStr !== leftHandSideHighlightHtml) {
                    setLeftHandSideHighlightHtml(patchedLeftStr);
                }

                // we only change the hightlighting on the BACKGROUND div so we don't mess up typing/cursor
                if (patchedRightStr !== rightHandSideHighlightHtml) {
                    setRightHandSideHighlightHtml(patchedRightStr);
                }
            },
            [leftHandSideHighlightHtml, rightHandSideHighlightHtml]
        );

        const onChangeHandler = useCallback(
            (newText: string) => {
                setTextAreaContent(newText);
                refreshDiffCallback(props.original, newText);
            },
            [setTextAreaContent, refreshDiffCallback, props]
        );

        useEffect(() => {
            if (props.modified?.length > 0 && textAreaContent.length === 0) {
                // initialization only
                onChangeHandler(props.modified);
            }
        }, [textAreaContent, props, onChangeHandler]);

        return (
            <ScrollSync>
                <div className="rs-editable-compare-container">
                    <div className="rs-editable-stacked-container">
                        <ScrollSyncPane>
                            <div
                                ref={staticDiffRef}
                                className="rs-editable-compare-base rs-editable-compare-static"
                                contentEditable={false}
                                dangerouslySetInnerHTML={{
                                    __html: DOMPurify.sanitize(props.original),
                                }}
                            />
                        </ScrollSyncPane>

                        <ScrollSyncPane>
                            <div
                                ref={editDiffBackgroundRef}
                                className="rs-editable-compare-base rs-editable-compare-background"
                                dangerouslySetInnerHTML={{
                                    __html: DOMPurify.sanitize(
                                        leftHandSideHighlightHtml
                                    ),
                                }}
                            />
                        </ScrollSyncPane>
                    </div>
                    <div className="rs-editable-stacked-container">
                        <ScrollSyncPane>
                            <textarea
                                className="rs-editable-compare-base rs-editable-compare-edit"
                                ref={editDiffRef}
                                value={textAreaContent}
                                onChange={(e) => {
                                    onChangeHandler(e.target.value);
                                }}
                                onBlur={(e) => {
                                    checkTextAreaJson(
                                        e?.target?.value,
                                        "edited text",
                                        editDiffRef
                                    );
                                }}
                            />
                        </ScrollSyncPane>

                        <ScrollSyncPane>
                            <div
                                ref={editDiffBackgroundRef}
                                className="rs-editable-compare-base rs-editable-compare-background"
                                dangerouslySetInnerHTML={{
                                    // the extra `<br/>` is required for some odd reason.
                                    // It's in the shadowdom of the textarea.
                                    __html: `${DOMPurify.sanitize(
                                        rightHandSideHighlightHtml
                                    )}<br/>`,
                                }}
                            />
                        </ScrollSyncPane>
                    </div>
                </div>
            </ScrollSync>
        );
    }
);
