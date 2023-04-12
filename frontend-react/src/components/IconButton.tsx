import { Button, Icon } from "@trussworks/react-uswds";
import { ButtonProps } from "@trussworks/react-uswds/lib/components/Button/Button";
import { IconProps } from "@trussworks/react-uswds/lib/components/Icon/Icon";
import classnames from "classnames";
import React from "react";

export type IconButtonProps = Omit<ButtonProps, "type" | "children"> &
    React.HTMLAttributes<HTMLButtonElement> & {
        type?: ButtonProps["type"];
        iconProps: IconProps & {
            icon: Exclude<keyof typeof Icon, "prototype">;
        };
    };

// FUTURE_TODO: Investigate patching trussworks Button to accept forwardRef.
/**
 * An icon can be given either as a name of a valid icon in iconProps.icon or as a custom
 * override via children.
 */
export const IconButton = React.forwardRef(
    (
        {
            children,
            iconProps: _iconProps,
            className,
            type = "button",
            ...props
        }: IconButtonProps,
        ref: React.ForwardedRef<any>
    ): React.ReactElement => {
        const classes = classnames("usa-icon-button", className);
        const { icon, ...iconProps } = _iconProps ?? {};
        const IconComponent = Icon[icon];

        return (
            <div ref={ref}>
                <Button type={type} {...props} className={classes}>
                    <IconComponent {...iconProps} />
                    {children}
                </Button>
            </div>
        );
    }
);
