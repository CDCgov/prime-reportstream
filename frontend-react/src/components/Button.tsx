import { Button as OrigButton } from "@trussworks/react-uswds";
import { ButtonProps as OrigButtonProps } from "@trussworks/react-uswds/lib/components/Button/Button";
import React from "react";

import { withUnnestedProps } from "../utils/StorybookUtils";
import {
    TooltipTrigger,
    TooltipContent,
    Tooltip,
    TooltipContentProps,
    TooltipProps,
    TooltipTriggerProps,
} from "../shared/Tooltip";

export type ButtonProps = Omit<OrigButtonProps, "type"> & {
    type?: OrigButtonProps["type"];
    className?: string;
    tooltip?: {
        tooltipContentProps?: TooltipContentProps;
        tooltipProps?: TooltipProps;
        tooltipTriggerProps?: TooltipTriggerProps;
    };
};

/**
 * Wrapper for trussworks Button that allows for tooltip functionality. Default
 * type is button (safe for general use). Provide custom type for use in forms.
 */
export const Button = React.forwardRef(
    ({ tooltip, type = "button", ...props }: ButtonProps, ref: any) => {
        const button = (
            <span className="display-inline-block" ref={ref}>
                <OrigButton type={type} {...props} />
            </span>
        );

        if (tooltip) {
            return (
                <Tooltip placement="top" {...tooltip.tooltipProps}>
                    <TooltipTrigger
                        asChild={true}
                        {...tooltip.tooltipTriggerProps}
                    >
                        {button}
                    </TooltipTrigger>
                    <TooltipContent {...tooltip.tooltipContentProps} />
                </Tooltip>
            );
        }

        return button;
    }
);

// For storybook
export const UnnestedButton = React.forwardRef(
    withUnnestedProps<
        typeof Button,
        {
            tooltip__tooltipContentProps__children: string;
            tooltip__tooltipProps__open: boolean;
        }
    >(Button, ["tooltip__tooltipContentProps", "tooltip__tooltipProps"])
);
