import React, { ReactNode } from "react";
import classNames from "classnames";

export enum StaticAlertType {
    Success = "success",
    Error = "error",
    Warning = "warning",
    Slim = "slim",
}

interface StaticAlertProps {
    type: StaticAlertType | StaticAlertType[];
    heading: string;
    message?: string;
    children?: ReactNode;
}

export const StaticAlert = ({
    type,
    heading,
    message,
    children,
}: StaticAlertProps) => {
    type = Array.isArray(type) ? type : [type];

    const alertClasses = classNames({
        "usa-alert": true,
        "usa-alert--success": type.includes(StaticAlertType.Success),
        "usa-alert--error": type.includes(StaticAlertType.Error),
        "usa-alert--warning": type.includes(StaticAlertType.Warning),
        "usa-alert--slim": type.includes(StaticAlertType.Slim),
    });
    return (
        <div className={alertClasses} role="alert">
            <div className="usa-alert__body">
                {heading && <h4 className="usa-alert__heading">{heading}</h4>}
                <p className="usa-alert__text">{message}</p>
                {children}
            </div>
        </div>
    );
};
