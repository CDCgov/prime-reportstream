import { Icon } from "@trussworks/react-uswds";
import { IconProps } from "@trussworks/react-uswds/lib/components/Icon/Icon";
import classnames from "classnames";
import React from "react";

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
        {
            children,
            iconProps: _iconProps,
            className,
            ...props
        }: IconButtonProps,
        ref: React.ForwardedRef<any>
    ) => {
        const classes = classnames("usa-icon-button", className);
        const { icon, ...iconProps } = _iconProps ?? {};
        const IconComponent = Icon[icon];

        return (
            <Button {...props} className={classes} ref={ref}>
                <IconComponent {...iconProps} />
                {children}
            </Button>
        );
    }
);
