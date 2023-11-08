import React, { ReactNode, useState } from "react";
import { Icon, Tooltip } from "@trussworks/react-uswds";
import classnames from "classnames";

import styles from "./CodeSnippet.module.scss";

/**
 * Gets the text from the ReactNode object.
 */
const getNodeText = (node: React.ReactNode): string => {
    if (node == null) return "";

    switch (typeof node) {
        case "string":
        case "number":
            return node.toString();

        case "boolean":
            return "";

        case "object": {
            if (node instanceof Array) return node.map(getNodeText).join("");

            if ("props" in node) return getNodeText(node.props.children);
            return "";
        }

        default:
            console.warn("Unresolved `node` of type:", typeof node, node);
            return "";
    }
};

/**
 * Surround {highlightText} matches with span with special class (default: text-highlight).
 */
export function createHighlightedText(
    text: string,
    highlightText: string | RegExp | (string | RegExp)[],
    className = "text-highlight",
) {
    const highlightTextList = (
        Array.isArray(highlightText) ? highlightText : [highlightText]
    ).map((h) => {
        if (typeof h === "string") {
            return new RegExp(h, "g");
        }
        return h;
    });
    /**
     * Merge overlapping matches and ignore redundant ones to get our final
     * list of substring index pairs.
     */
    const segments = highlightTextList
        .flatMap((h) => Array.from(text.matchAll(h)))
        .sort((a, b) => {
            const aI = a.index ?? 0;
            const bI = b.index ?? 0;
            if (aI > bI) {
                return 1;
            } else if (aI < bI) {
                return -1;
            }
            return 0;
        })
        .map((m) => [m.index ?? 0, (m.index ?? 0) + m[0].length])
        .reduce(
            (p, [startI, endI]) => {
                const prev = p.at(-1);

                if (!prev) {
                    p.push([startI, endI]);
                    return p;
                }

                const [, prevEndI] = prev;

                if (startI <= prevEndI) {
                    if (endI > prevEndI) {
                        prev[1] = endI;
                    }
                } else {
                    p.push([startI, endI]);
                }

                return p;
            },
            [] as [number, number][],
        );

    const split = [];
    if (segments.length) {
        let i = 0;
        for (const [startI, endI] of segments) {
            if (i !== startI) {
                split.push(
                    <React.Fragment key={i}>
                        {text.substring(i, startI)}
                    </React.Fragment>,
                );
                i = startI;
            }
            split.push(
                <span key={i} className={className}>
                    {text.substring(i, endI)}
                </span>,
            );
            i = endI;
        }
        if (i !== text.length) {
            split.push(
                <React.Fragment key={i}>{text.substring(i)}</React.Fragment>,
            );
        }
    } else {
        return text;
    }
    return split;
}

interface CodeSnippetProps {
    children?: ReactNode;
    highlightText?: string | RegExp | (string | RegExp)[];
}

export const CodeSnippet = ({ children, highlightText }: CodeSnippetProps) => {
    const [isCopied, setIsCopied] = useState(false);
    const tooltipText = isCopied ? "Copied" : "Copy to clipboard";

    const copyToClipboard = (snippet: ReactNode) => {
        navigator.clipboard.writeText(getNodeText(snippet));
        setIsCopied(true);
        // alert(`You have copied "${getNodeText(snippet)}"`);
    };

    React.useEffect(() => {
        if (isCopied) {
            setIsCopied(false);
        }
    }, [isCopied]);

    return (
        <pre className={classnames(styles.CodeSnippet, "grid-row")}>
            <code className="tablet:grid-col code_snippet">
                {highlightText
                    ? createHighlightedText(
                          children?.toString() ?? "",
                          highlightText,
                      )
                    : children}
            </code>
            <Tooltip
                className="fixed-tooltip"
                position="top"
                label={tooltipText}
                onClick={() => copyToClipboard(children)}
            >
                <Icon.ContentCopy className="position" />
            </Tooltip>
        </pre>
    );
};
