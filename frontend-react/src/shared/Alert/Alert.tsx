// AutoUpdateFileChromatic
import React, { AriaRole } from "react";
import classnames from "classnames";
import { Alert as OrigAlert } from "@trussworks/react-uswds";

import Icon, { IconName, SubcomponentIconProp } from "../Icon/Icon";

import styles from "./Alert.module.scss";

type OrigAlertProps = React.ComponentProps<typeof OrigAlert>;

export interface AlertProps
    extends Omit<OrigAlertProps, "validation" | "type" | "headingLevel"> {
    icon?: SubcomponentIconProp;
    type: OrigAlertProps["type"] | "tip";
    headingLevel?: OrigAlertProps["headingLevel"] | "div";
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
        case "tip":
            return "Lightbulb";

        default:
            return "Info";
    }
}

/**
 * Enhancement of Trussworks' Alert. Supports custom icons specified
 * by name or object. Supports reportstream's "tip" style as type. Applies
 * appropriate aria role and label. Will always enable validation on
 * Trussworks' Alert so that children are not wrapped in a potentially
 * errorneous paragraph element. The validation class name does not
 * seem to have a visible effect on Alert appearance so is currently
 * safe. Heading level accepts "div" (opt-out of using header element).
 * @see https://designsystem.digital.gov/components/alert/#alert-aria-roles
 */
export const Alert = ({
    role,
    children,
    headingLevel = "div",
    type: _type,
    className,
    slim = _type === "tip",
    icon,
    noIcon,
    ...props
}: AlertProps & React.HTMLAttributes<HTMLDivElement>): React.ReactElement => {
    const classes = classnames(
        styles["usa-alert"],
        {
            [styles["usa-alert--tip"]]: _type === "tip",
        },
        className,
    );

    // convert "tip" to "info"
    const type = _type === "tip" ? "info" : _type;
    const ariaRole = role ?? getAriaRole(type);
    const ariaLabel = ariaRole === "region" ? "Information" : undefined;
    const alertIcon =
        icon == null || typeof icon === "string" ? (
            <Icon name={icon ?? getIconName(_type)} />
        ) : (
            icon
        );

    return (
        <OrigAlert
            aria-label={ariaLabel}
            className={classes}
            slim={slim}
            type={type}
            role={ariaRole}
            validation
            headingLevel={headingLevel as any}
            noIcon={noIcon}
            {...props}
        >
            {!noIcon && alertIcon}
            <div className="usa-alert__text">{children}</div>
        </OrigAlert>
    );
};

export default Alert;

/**
 * Simplified icon prop for storybook
 */
export const AlertSimple = (
    props: Omit<AlertProps, "icon"> & { icon?: IconName },
) => <Alert {...props} />;
