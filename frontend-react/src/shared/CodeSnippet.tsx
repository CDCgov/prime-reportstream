import classNames from "classnames";
import React, { AriaRole, useId, useState } from "react";

import { IconButton, IconButtonProps } from "../components/IconButton";
import {
    TooltipContentProps,
    TooltipProps,
    TooltipTriggerProps,
} from "../components/Tooltip";
import { isReactNode } from "../utils/misc";

/**
 * Surround {highlightText} matches with span with special class (default: text-highlight).
 */
export function createHighlightedText(
    text: string,
    highlightText: string | RegExp | (string | RegExp)[],
    className = "text-highlight"
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
        .reduce((p, [startI, endI], i) => {
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
        }, [] as [number, number][]);

    const split = [];
    if (segments.length) {
        let i = 0;
        for (const [startI, endI] of segments) {
            if (i !== startI) {
                split.push(
                    <React.Fragment key={i}>
                        {text.substring(i, startI)}
                    </React.Fragment>
                );
                i = startI;
            }
            split.push(
                <span key={i} className={className}>
                    {text.substring(i, endI)}
                </span>
            );
            i = endI;
        }
        if (i !== text.length) {
            split.push(
                <React.Fragment key={i}>{text.substring(i)}</React.Fragment>
            );
        }
    } else {
        return text;
    }
    return split;
}

export interface CodeSnippetProps extends React.HTMLAttributes<HTMLElement> {
    children: React.ReactNode;
    isBackground?: boolean;
    isBlock?: boolean;
    to?: string | React.FunctionComponent<React.HTMLAttributes<HTMLElement>>;
    isButtonVisible?: boolean;
    onButtonClick?: React.MouseEventHandler<HTMLElement>;
    codeProps?: Partial<React.HTMLAttributes<HTMLElement>>;
    buttonOrProps?: Partial<IconButtonProps> | React.ReactNode;
    highlightText?: string | RegExp | (string | RegExp)[];
    figure?: {
        role?: AriaRole;
        label: string;
        figureProps?: React.HTMLAttributes<HTMLElement>;
        figureCaptionProps: React.HTMLAttributes<HTMLElement> & {
            children: Exclude<React.ReactNode, null | undefined>;
        };
    };
    tooltip?:
        | boolean
        | {
              tooltipContentProps?: TooltipContentProps;
              tooltipProps?: TooltipProps;
              tooltipTriggerProps?: TooltipTriggerProps;
          };
}

function getTooltipText(isOpen: boolean) {
    if (isOpen) {
        return "Copied";
    }

    return "Copy to clipboard";
}

/**
 * Has two display modes: inline and block. Inline mode has no background, no button, and all text is
 * highlighted. Block mode has a background, button, and only specified text is highlighted. Inline is
 * the default and overridable via isBlock prop. A background toggle is available via isBackground as an
 * override depending on mode. When background is enabled, only specified text is highlighted. When
 * background is disabled, all text is highlighted. Using any figure props automatically makes the
 * CodeSnippet a block, and cannot be overriden.
 *
 * IMPORTANT: Please adhere to W3C rules and do not use this element in block mode within an inline
 * context (ex: `<p>`).
 *
 * Text fragments can be highlighted with either strings or regular expressions.
 *
 * Container element and props are customizable. Code element props are customizable. Button can be replaced
 * entirely with one provided, or customized with props. Figure and figurecaption element props are
 * customizable. Tooltip
 *
 * ```
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
 * |CAPTION               |
 * +----------------------+
 * ```
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
    figure,
    tooltip,
    ...props
}: CodeSnippetProps) {
    const Container = to ?? "span";
    const isBlockShowing =
        !!figure || (isBlock !== undefined ? isBlock : false);
    const isButtonShowing = isButtonVisible ?? isBlockShowing;
    const isBackgroundShowing = isBackground ?? isBlockShowing;
    const isHighlightText =
        !!highlightText && (isBlockShowing || isBackgroundShowing);
    const isTooltipEnabled = tooltip !== false;
    const ariaId = useId();
    const [isCopied, setIsCopied] = useState(false);
    const [isTooltipShowing, setIsTooltipShowing] = useState(false);
    const tooltipOptions: IconButtonProps["tooltip"] =
        typeof tooltip === "object"
            ? tooltip
            : ({
                  tooltipProps: {
                      offsetBy: -13,
                      onOpenChange: setIsTooltipShowing,
                  },
                  tooltipContentProps: {
                      children: getTooltipText(isCopied),
                  },
              } as IconButtonProps["tooltip"]);
    const defaultHandler = React.useCallback(
        (_: React.MouseEvent<HTMLElement>) => {
            navigator.clipboard.writeText(children?.toString() ?? "");
            setIsCopied(true);
        },
        [children]
    );

    // Reset tooltip text after copy & hover deactivated
    React.useEffect(() => {
        if (!isTooltipShowing && isCopied) {
            setIsCopied(false);
        }
    }, [isCopied, isTooltipShowing]);

    const body = (
        <Container
            role={figure?.role ?? "img"}
            aria-label={figure?.label}
            {...props}
            className={classNames(
                "code_snippet",
                isBlockShowing && "code_snippet--block",
                isBlockShowing &&
                    !isBackgroundShowing &&
                    "code_snippet--block-no_background",
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
                {!isHighlightText
                    ? children
                    : createHighlightedText(
                          children?.toString() ?? "",
                          highlightText
                      )}
            </code>

            {isButtonShowing &&
                (isReactNode(buttonOrProps) ? (
                    buttonOrProps
                ) : (
                    <IconButton
                        type="button"
                        onClick={onButtonClick ?? defaultHandler}
                        tooltip={isTooltipEnabled ? tooltipOptions : undefined}
                        {...buttonOrProps}
                        iconProps={{
                            icon: "ContentCopy",
                            ...buttonOrProps?.iconProps,
                        }}
                        className={classNames(
                            "code_snippet--button",
                            buttonOrProps.className
                        )}
                    />
                ))}
        </Container>
    );

    if (figure) {
        return (
            <figure {...figure.figureProps}>
                {body}
                <figcaption id={ariaId} {...figure.figureCaptionProps} />
            </figure>
        );
    }

    return body;
}
