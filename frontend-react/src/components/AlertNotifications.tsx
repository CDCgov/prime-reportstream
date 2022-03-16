import { Alert } from "@trussworks/react-uswds";
import { toast } from "react-toastify";
import React from "react";

/** you probably showAlertNotification **/
export const showNotification = (children: JSX.Element) => {
    try {
        // id will de-dup. just use whole message as id
        const toastId = JSON.stringify(children.props).substr(0, 512);
        toast(children, {
            toastId,
            autoClose: 5000,
            closeButton: false,
            // limit: 2,
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
    title?: React.ReactNode | string,
    message?: React.ReactNode | string
) => {
    showNotification(<Alert type={type} heading={title} children={message} />);
};

export const showError = (
    message = "Please check for missing data or typos.",
    title = "Problems saving data to server"
) => {
    const err_msg = message.substr(0, 512);
    showNotification(<Alert type="error" heading={title} children={err_msg} />);
};
