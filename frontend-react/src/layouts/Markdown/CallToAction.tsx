import classNames from "classnames";

import { USSmartLink } from "../../components/USLink";
import { Icon, IconName } from "../../shared";

export interface CallToActionItem {
    label: string;
    href: string;
    style?: string;
    icon?: IconName;
}

export type CallToActionProps = React.PropsWithChildren<CallToActionItem>;

export const CallToAction = ({
    label,
    href,
    style,
    icon,
}: CallToActionProps) => {
    const classname = classNames(
        "usa-button",
        style ? `usa-button--${style}` : null,
    );
    return (
        <USSmartLink href={href} className={classname}>
            {label}
            {icon ? <Icon name={icon} className="usa-icon--auto" /> : null}
        </USSmartLink>
    );
};
