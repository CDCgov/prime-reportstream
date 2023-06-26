import classnames from "classnames";

import { IconName, IconProps, Icon, SubcomponentIconProp } from "../Icon/Icon";

import { AlertProps } from "./Alert";
import styles from "./Alert.module.scss";

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

export type AlertIconProps = React.PropsWithChildren<{
    type: AlertProps["type"];
    icon?: SubcomponentIconProp;
}>;

export type AlertIconSimpleProps = React.PropsWithChildren<{
    type: AlertProps["type"];
    icon?: IconName;
}>;

/**
 * Will choose alert icon default unless otherwise specified.
 */
function AlertIcon({ type, icon }: AlertIconProps) {
    const { name, className, ...props }: Partial<IconProps> =
        typeof icon === "object" ? icon : {};
    const classes = classnames(styles["usa-alert__icon"], className);
    const iconName: IconName =
        name ?? (typeof icon === "string" ? icon : getIconName(type));

    return <Icon name={iconName} className={classes} {...props} />;
}

export default AlertIcon;

/**
 * Simplified icon prop for storybook
 */
export const AlertIconSimple = (
    props: Omit<AlertIconProps, "icon"> & { icon?: IconName }
) => <AlertIcon {...props} />;
