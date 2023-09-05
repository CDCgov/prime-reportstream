// AutoUpdateFileChromatic
import React from "react";
import { Icon as OrigIcon } from "@trussworks/react-uswds";
import { IconProps as OrigIconProps } from "@trussworks/react-uswds/lib/components/Icon/Icon";
import classNames from "classnames";

import rsSprite from "./reportstream-sprite.svg?url";

export type RSIconName =
    | "rs-mpox"
    | "rs-hl7"
    | "rs-fhir"
    | "rs-healthkit"
    | "rs-covid"
    | "rs-csv";

export interface RSIconProps extends React.HTMLAttributes<SVGElement> {
    name: RSIconName;
    size?: 3 | 4 | 5 | 6 | 7 | 8 | 9;
}

export function RSIcon({ name, className, size, ...props }: RSIconProps) {
    const classnames = classNames(
        "usa-icon",
        size && `usa-icon--size-${size}`,
        className,
    );
    return (
        <svg className={classnames} {...props}>
            <use href={`${rsSprite}#${name.replace("rs-", "")}`} />
        </svg>
    );
}

export type OrigIconName = Exclude<keyof typeof OrigIcon, "prototype">;
export type IconName = OrigIconName | RSIconName;

export interface IconProps extends OrigIconProps {
    name: IconName;
}
export type SubcomponentIconProp = IconProps["name"] | JSX.Element;

/**
 * Allows for using icons by their string name instead of importing
 * the Icon class.
 */
export function Icon({ name, ...props }: IconProps) {
    if (name.startsWith("rs-")) {
        return <RSIcon name={name as RSIconName} {...props} />;
    }

    const IconComponent = OrigIcon[name as OrigIconName];
    return <IconComponent aria-label={name} {...props} />;
}

export default Icon;
