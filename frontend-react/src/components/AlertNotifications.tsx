import { toast } from "react-toastify";
import React from "react";

/** you probably showAlertNotification **/
export const showNotification = (children: JSX.Element) => {
    try {
        // id will de-dup, get the title+message remove any non-alphanum (incl whitespace) and limit size
        const toastId =
            `id_${children?.props?.heading}_${children?.props?.children}.`
                .replace(/\W/gi, "_")
                .substring(0, 512);
        toast(children, {
            toastId,
            autoClose: 5000,
            delay: 10,
            closeButton: false,
            position: "bottom-center",
            hideProgressBar: true,
        });
        toast.clearWaitingQueue(); // don't pile up messages
    } catch (err: any) {
        console.error(err, err.stack);
    }
};

export const showAlertNotification = (
    type: "success" | "warning" | "error" | "info",
    title: React.ReactNode | string = "",
    message: React.ReactNode | string = ""
) => {
    // basically the USWDS Alert UI
    showNotification(
        <div
            className={`usa-alert usa-alert--slim usa-alert--no-icon usa-alert--${type} rs-alert-toast`}
            data-testid="alerttoast"
        >
            <div className="usa-alert__body">
                {title && <h4 className="usa-alert__heading">{title}</h4>}
                {message && <p className="usa-alert__text">{message}</p>}
            </div>
        </div>
    );
};

export const showError = (
    message = "Please check for missing data or typos.",
    title = "Problems saving data to server"
) => {
    const err_msg = message.substring(0, 512);
    showNotification(
        <div
            className={`usa-alert usa-alert--slim usa-alert--no-icon usa-alert--error rs-alert-toast`}
            data-testid="alerttoast"
        >
            <div className="usa-alert__body">
                {title && <h4 className="usa-alert__heading">{title}</h4>}
                {err_msg && <p className="usa-alert__text">{err_msg}</p>}
            </div>
        </div>
    );
};
