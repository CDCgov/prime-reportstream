import React, { ReactNode, useCallback, useEffect, useState } from "react";
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

    /**
     * Cached component that renders tooltip so that changing isCopied status
     * causes it to remount (thus forcing tooltip position recalculation).
     */
    const CopyTooltip = useCallback(
        ({ children }: React.PropsWithChildren) => (
            <Tooltip
                className="fixed-tooltip"
                position="top"
                label={isCopied ? "Copied" : "Copy to clipboard"}
                onClick={() => {
                    navigator.clipboard.writeText(getNodeText(children));
                    setIsCopied(true);
                }}
            >
                {children}
            </Tooltip>
        ),
        [isCopied],
    );

    useEffect(() => {
        let timeout: number | undefined;
        if (isCopied) {
            setTimeout(() => setIsCopied(false), 3000);
        }

        return () => clearTimeout(timeout);
    }, [isCopied]);

    return (
        <pre className={classnames(styles.CodeSnippet, "grid-row")}>
            <code className="tablet:grid-col code_snippet">{children}</code>
            <CopyTooltip>
                <Icon.ContentCopy className="position" />
            </CopyTooltip>
        </pre>
    );
};
