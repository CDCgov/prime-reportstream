import { toast } from "react-toastify";
import React from "react";

export const showAlertNotification = (
    type: "success" | "warning" | "error" | "info",
    message: React.ReactNode | string = "",
) => {
    try {
        const toastId = `id_${message}`.replace(/\W/gi, "_").substring(0, 512);

        // basically the USWDS Alert UI
        toast(
            <div
                className={`usa-alert usa-alert--slim usa-alert--no-icon usa-alert--${type} rs-alert-toast`}
                data-testid="alerttoast"
            >
                <div className="usa-alert__body">
                    {message && (
                        <h2 className="usa-alert__heading">{message}</h2>
                    )}
                </div>
            </div>,
            {
                toastId,
                autoClose: 5000,
                delay: 10,
                closeButton: false,
                position: "bottom-center",
                hideProgressBar: true,
            },
        );
        toast.clearWaitingQueue(); // don't pile up messages
    } catch (err: any) {
        console.error(err, err.stack);
    }
};

export const showError = (message: string) => {
    showAlertNotification("error", message);
    console.trace("ShowError", message);
};
