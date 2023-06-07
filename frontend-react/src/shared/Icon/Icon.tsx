import React from "react";
import { Icon as OrigIcon } from "@trussworks/react-uswds";
import { IconProps as OrigIconProps } from "@trussworks/react-uswds/lib/components/Icon/Icon";

export type IconName = string & Exclude<keyof typeof OrigIcon, "prototype">;

export type IconProps = React.PropsWithChildren<
    {
        name: IconName;
    } & OrigIconProps
>;

/**
 * Allows for using icons by their string name instead of importing
 * the Icon class.
 */
export function Icon({ name, ...props }: IconProps) {
    const IconComponent = OrigIcon[name];

    return <IconComponent {...props} />;
}

export default Icon;
