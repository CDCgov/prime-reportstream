import React, { ReactNode, useEffect, useState } from "react";
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

interface CodeSnippetProps {
    children?: ReactNode;
}

export const CodeSnippet = ({ children }: CodeSnippetProps) => {
    const [isCopied, setIsCopied] = useState(false);
    const tooltipText = isCopied ? "Copied" : "Copy to clipboard";

    const copyToClipboard = (snippet: ReactNode) => {
        navigator.clipboard.writeText(getNodeText(snippet));
        setIsCopied(true);
    };

    useEffect(() => {
        if (isCopied) {
            setIsCopied(false);
        }
    }, [isCopied]);

    return (
        <pre className={classnames(styles.CodeSnippet, "grid-row")}>
            <code className="tablet:grid-col code_snippet">{children}</code>
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
