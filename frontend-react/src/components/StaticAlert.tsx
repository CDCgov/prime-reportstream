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
        "usa-alert--error": type === "error",
    });
    return (
        <div className={alertClasses} role="alert">
            <div className="usa-alert__body">
                <h4 className="usa-alert__heading">{heading}</h4>
                <p className="usa-alert__text">{message}</p>
                {children}
            </div>
        </div>
    );
};
