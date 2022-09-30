import React, { ReactNode } from "react";
import classNames from "classnames";

interface StaticAlertProps {
    type: string;
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
    const alertClasses = classNames({
        "usa-alert": true,
        "usa-alert--success": type === "success",
        "usa-alert--success usa-alert--slim": type === "success slim",
        "usa-alert--error": type === "error",
        "usa-alert--error usa-alert--slim": type === "error slim",
        "usa-alert--warning": type === "warning",
        "usa-alert--warning usa-alert--slim": type === "warning slim",
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
