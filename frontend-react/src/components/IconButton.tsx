import { Icon } from "@trussworks/react-uswds";
import { IconProps } from "@trussworks/react-uswds/lib/components/Icon/Icon";
import classnames from "classnames";
import React from "react";

import { withUnnestedProps } from "../utils/StorybookUtils";

import { Button, ButtonProps } from "./Button";

export type IconButtonProps = Omit<ButtonProps, "children"> &
    React.HTMLAttributes<HTMLButtonElement> & {
        iconProps: IconProps & {
            icon: Exclude<keyof typeof Icon, "prototype">;
        };
    };

/**
 * An icon is specified via the iconProps.icon prop. Other icon element props are
 * available to customize in iconProps.
 */
export const IconButton = React.forwardRef(
    (
        { children, iconProps, className, ...props }: IconButtonProps,
        ref: React.ForwardedRef<any>
    ) => {
        if (!iconProps?.icon) {
            throw new Error("IconButton component requires an icon.");
        }
        const { icon, ...icProps } = iconProps;
        const classes = classnames("usa-icon-button", className);
        const IconComponent = Icon[icon];

        return (
            <Button {...props} className={classes} ref={ref}>
                <IconComponent {...icProps} />
                {children}
            </Button>
        );
    }
);

// For storybook
export const UnnestedIconButton = React.forwardRef(
    withUnnestedProps<
        typeof IconButton,
        {
            // SB can't seem to handle a direct reference, so we use a utility
            // which likely returns the type representation directly through
            // transformation (in this case nothing is being changed).
            iconProps__icon: Exclude<IconButtonProps["iconProps"]["icon"], "">;
            tooltip__tooltipContentProps__children: string;
        }
    >(IconButton, ["iconProps", "tooltip__tooltipContentProps"])
);
