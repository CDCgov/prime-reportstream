import { Icon, Tooltip } from "@trussworks/react-uswds";
import classnames from "classnames";
import {
    PropsWithChildren,
    ReactNode,
    useCallback,
    useEffect,
    useRef,
    useState,
} from "react";

import styles from "./CodeSnippet.module.scss";

interface CodeSnippetProps extends PropsWithChildren {
    children?: ReactNode;
}

const CodeSnippet = ({ children }: CodeSnippetProps) => {
    const [isCopied, setIsCopied] = useState(false);
    const containerRef = useRef<HTMLElement>(null);

    /**
     * Cached component that renders tooltip so that changing isCopied status
     * causes it to remount (thus forcing tooltip position recalculation).
     */
    const CopyTooltip = useCallback(
        () => (
            <Tooltip
                className="fixed-tooltip"
                position="left"
                label={isCopied ? "Copied" : "Copy to clipboard"}
                onClick={() => {
                    if (containerRef.current?.textContent)
                        void navigator.clipboard.writeText(
                            containerRef.current.textContent,
                        );

                    setIsCopied(true);
                }}
            >
                <Icon.ContentCopy className="position" />
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
            <code ref={containerRef} className="tablet:grid-col code_snippet">
                {children}
            </code>
            <CopyTooltip></CopyTooltip>
        </pre>
    );
};

export default CodeSnippet;
