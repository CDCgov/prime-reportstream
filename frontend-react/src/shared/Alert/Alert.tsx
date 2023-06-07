import React, { AriaRole } from "react";
import classnames from "classnames";
import { Alert as OrigAlert } from "@trussworks/react-uswds";

import { IconName, IconProps, Icon } from "../Icon/Icon";

import styles from "./Alert.module.scss";

export interface AlertProps
    extends Omit<React.ComponentProps<typeof OrigAlert>, "validation"> {
    tip?: boolean;
    icon?: IconName | Partial<IconProps>;
}

export function getAriaRole(type: AlertProps["type"]): AriaRole {
    switch (type) {
        case "error":
            return "alert";
        case "success":
            return "status";
        default:
            return "region";
    }
}

export function getIconName(type: AlertProps["type"]): IconName {
    switch (type) {
        case "error":
            return "Error";
        case "success":
            return "CheckCircle";
        case "warning":
            return "Warning";

        default:
            return "Info";
    }
}

export type AlertIconProps = React.PropsWithChildren<{
    type: AlertProps["type"];
    icon: AlertProps["icon"];
}>;

/**
 * Will choose alert icon default unless otherwise specified.
 */
export function AlertIcon({ type, icon }: AlertIconProps) {
    const { name, className, ...props }: Partial<IconProps> =
        typeof icon === "object" ? icon : {};
    const classes = classnames(styles["usa-alert__icon"], className);
    const iconName: IconName =
        name ?? (typeof icon === "string" ? icon : getIconName(type));

    return <Icon name={iconName} className={classes} {...props} />;
}

/**
 * Enhancement of Trussworks' Alert. Supports custom icons specified
 * by name or object. Supports reportstream's "tip" style. Applies
 * appropriate aria role and label. Tip mode will enable slim mode
 * unless explicitly specified. Will always enable validation on
 * Trussworks' Alert so that children are not wrapped in a potentially
 * errorneous paragraph element. The validation class name does not
 * seem to have a visible effect on Alert appearance so is currently
 * safe.
 * @see https://designsystem.digital.gov/components/alert/#alert-aria-roles
 */
export const Alert = ({
    role,
    children,
    type,
    className,
    tip,
    slim = tip,
    icon,
    ...props
}: AlertProps & React.HTMLAttributes<HTMLDivElement>): React.ReactElement => {
    const classes = classnames(
        styles["usa-alert"],
        {
            [styles["usa-alert--tip"]]: tip,
        },
        className
    );

    const ariaRole = role ?? getAriaRole(type);
    const ariaLabel = ariaRole === "region" ? "Information" : undefined;

    return (
        <OrigAlert
            aria-label={ariaLabel}
            className={classes}
            slim={slim}
            type={type}
            role={ariaRole}
            validation
            {...props}
        >
            <AlertIcon type={type} icon={icon} />
            <div className="usa-alert__text">{children}</div>
        </OrigAlert>
    );
};

export default Alert;
