import React, { useCallback, useEffect, useState } from "react";
import { Icon, Tooltip } from "@trussworks/react-uswds";
import classnames from "classnames";

import styles from "./CodeSnippet.module.scss";

interface CodeSnippetProps extends React.PropsWithChildren {
    copyString: string;
}

export const CodeSnippet = ({ children, copyString }: CodeSnippetProps) => {
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
                    navigator.clipboard.writeText(copyString);
                    setIsCopied(true);
                }}
            >
                {children}
            </Tooltip>
        ),
        [isCopied, copyString],
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
