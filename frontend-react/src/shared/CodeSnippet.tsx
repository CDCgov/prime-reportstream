import { Icon } from "@trussworks/react-uswds";
import { IconButton } from "../components/IconButton";

export interface CodeSnippetProps {
    children: React.ReactNode;
    onButtonClick?: React.MouseEventHandler<HTMLElement>;
}

export function CodeSnippet({ children, onButtonClick }: CodeSnippetProps) {
    const defaultHandler = (_e: React.MouseEvent<HTMLElement>) => {
        navigator.clipboard.writeText(children?.toString() ?? "");
        console.log("DING", children?.toString());
    };

    console.log("default", defaultHandler, "custom", onButtonClick);

    return (
        <pre className="code-snippet">
            <code className="code-snippet--code">{children}</code>
            <IconButton
                className="code-snippet--button"
                type="button"
                unstyled={true}
                onClick={onButtonClick ?? defaultHandler}
            >
                <Icon.ContentCopy />
            </IconButton>
        </pre>
    );
}
