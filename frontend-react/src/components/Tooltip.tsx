import classNames from "classnames";
import React from "react";

export interface TooltipProps extends React.HTMLAttributes<HTMLDivElement> {
    isSet?: boolean;
    isVisible?: boolean;
    position?: "top" | "bottom" | "right" | "left" | string;
}

export const TOOLTIP_POSITIONS = ["top", "bottom", "right", "left"];

export const Tooltip = React.forwardRef(function (
    { isSet, isVisible, position = "top", ...props }: TooltipProps,
    ref: React.ForwardedRef<any>
) {
    const bestPosition = TOOLTIP_POSITIONS.includes(position)
        ? position
        : TOOLTIP_POSITIONS.find((p) => p.includes(`${position}-`)) ?? "top";

    return (
        <div
            ref={ref}
            {...props}
            className={classNames(
                "usa-tooltip__body",
                bestPosition && `usa-tooltip__body--${position}`,
                isSet && "is-set",
                isVisible && "is-visible",
                props.className
            )}
        />
    );
});
