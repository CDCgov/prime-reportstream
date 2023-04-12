import {
    autoPlacement,
    useFloating,
    useHover,
    useInteractions,
} from "@floating-ui/react";
import classNames from "classnames";
import React, { useState } from "react";

import { IconButton, IconButtonProps } from "../components/IconButton";
import { Tooltip } from "../components/Tooltip";
import { isReactNode } from "../utils/misc";

export interface CodeSnippetProps extends React.HTMLAttributes<HTMLElement> {
    children: React.ReactNode;
    isBackground?: boolean;
    isBlock?: boolean;
    to?: string | React.FunctionComponent<React.HTMLAttributes<HTMLElement>>;
    isButtonVisible?: boolean;
    onButtonClick?: React.MouseEventHandler<HTMLElement>;
    codeProps?: Partial<React.HTMLAttributes<HTMLElement>>;
    buttonOrProps?: Partial<IconButtonProps> | React.ReactNode;
    highlightText?: string | string[];
}

/**
 * Surround {highlightText} matches with span with special class (default: text-highlight).
 */
export function createHighlightedText(
    text: string,
    highlightText: string[],
    className = "text-highlight"
) {
    const regex = new RegExp(`(${highlightText.join("|")})`, "g");
    const matches = Array.from(text.matchAll(regex));
    const split = [];
    if (matches.length) {
        let i = 0;
        for (const match of matches) {
            if (match.index !== undefined) {
                if (i !== match.index) {
                    split.push(text.substring(i, match.index));
                    i = match.index;
                }
                split.push(
                    <span className={className}>
                        {text.substring(i, match.index + match[0].length)}
                    </span>
                );
                i = match.index + match[0].length;
            }
        }
        if (i !== text.length) {
            split.push(text.substring(i));
        }
    } else {
        split.push(text);
    }
    return <>{split}</>;
}

/**
 * Has two display modes: inline and block. Inline mode has no background, no button, and all text is
 * highlighted. Block mode has a background, button, and only specified text is highlighted. Inline is
 * the default and overridable via isBlock prop. A background toggle is available via isBackground as an
 * override depending on mode. When background is enabled, only specified text is highlighted. When
 * background is disabled, all text is highlighted.
 *
 * Container element and props are customizable. Code element props are customizable. Button can be replaced
 * entirely with one provided, or customized with props.
 *
 * @example
 * Flex Layout Inline:
 * +---------------+------+
 * |   CHILDREN    |BUTTON|
 * +---------------+------+
 * |[ ] SCROLL     |      |
 * +----------------------+
 * Flex Layout Block:
 * +---------------+------+
 * |               |BUTTON|
 * |   CHILDREN    +------+
 * |               |      |
 * +---------------+------+
 * |[ ] SCROLL     |      |
 * +----------------------+
 */
export function CodeSnippet({
    children,
    onButtonClick,
    isButtonVisible,
    isBlock,
    to,
    className,
    isBackground,
    codeProps = {},
    buttonOrProps = {},
    highlightText,
    ...props
}: CodeSnippetProps) {
    const defaultHandler = (_e: React.MouseEvent<HTMLElement>) => {
        navigator.clipboard.writeText(children?.toString() ?? "");
    };
    const Container = to ?? "pre";
    const isButtonShowing =
        isButtonVisible !== undefined
            ? isButtonVisible
            : isBlock
            ? true
            : false;
    const isBlockShowing = isBlock !== undefined ? isBlock : false;
    const isBackgroundShowing =
        isBackground !== undefined ? isBackground : isBlockShowing;
    const highlightTextList =
        isBlockShowing || isBackgroundShowing
            ? Array.isArray(highlightText)
                ? highlightText
                : highlightText !== undefined
                ? [highlightText]
                : []
            : [];

    const [isOpen, setIsOpen] = useState(false);
    const { x, y, strategy, refs, context } = useFloating({
        open: isOpen,
        onOpenChange: setIsOpen,
        middleware: [autoPlacement()],
        placement: "top",
    });
    const hover = useHover(context);
    const { getReferenceProps, getFloatingProps } = useInteractions([hover]);
    console.log(context.placement);

    return (
        <Container
            {...props}
            className={classNames(
                "code_snippet",
                isBlockShowing && "code_snippet--block",
                isBlockShowing &&
                    !isBackgroundShowing &&
                    "code_snippet--no_background",
                !isBlockShowing &&
                    isBackgroundShowing &&
                    "code_snippet--background",
                className
            )}
        >
            <code
                {...codeProps}
                className={classNames(
                    "code_snippet--code",
                    codeProps.className
                )}
            >
                {highlightTextList.length === 0
                    ? children
                    : createHighlightedText(
                          children?.toString() ?? "",
                          highlightTextList
                      )}
            </code>
            {isButtonShowing &&
                (isReactNode(buttonOrProps) ? (
                    buttonOrProps
                ) : (
                    <div>
                        <IconButton
                            type="button"
                            onClick={onButtonClick ?? defaultHandler}
                            {...buttonOrProps}
                            {...getReferenceProps({
                                ref: refs.setReference,
                            })}
                            iconProps={{
                                icon: "ContentCopy",
                                ...buttonOrProps?.iconProps,
                            }}
                            className={classNames(
                                "code_snippet--button",
                                buttonOrProps.className
                            )}
                        />
                        {isOpen && (
                            <Tooltip
                                isSet={isOpen}
                                isVisible={isOpen}
                                position={context.placement}
                                ref={refs.setFloating}
                                style={{
                                    position: strategy,
                                    top: y ?? 0,
                                    left: x ?? 0,
                                }}
                                {...getFloatingProps()}
                            >
                                Copy to clipboard
                            </Tooltip>
                        )}
                    </div>
                ))}
        </Container>
    );
}
