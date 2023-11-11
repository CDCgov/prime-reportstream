import { toast } from "react-toastify";
import React, { createContext, useCallback, useContext, useMemo } from "react";

import { useSessionContext } from "../Session";

export interface ToastCtx {
    toast: (
        msgOrErr: Error | React.ReactNode,
        type: "success" | "warning" | "error" | "info",
    ) => void;
}

export const ToastContext = createContext<ToastCtx>({ toast: () => void 0 });

export const showToast = (
    message: React.ReactNode,
    type: "success" | "warning" | "error" | "info",
) => {
    const toastId = `id_${message}`.replace(/\W/gi, "_").substring(0, 512);
    const msg = message instanceof Error ? message.message : message;

    // basically the USWDS Alert UI
    toast(
        <div
            className={`usa-alert usa-alert--slim usa-alert--no-icon usa-alert--${type} rs-alert-toast`}
            data-testid="alerttoast"
        >
            <div className="usa-alert__body">
                {msg && <h2 className="usa-alert__heading">{msg}</h2>}
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
};

export const useToast = () => useContext(ToastContext);

export function ToastProvider({ children }: React.PropsWithChildren) {
    const { rsconsole } = useSessionContext();
    const fn = useCallback(
        (
            msgOrErr: Error | React.ReactNode,
            type: "success" | "warning" | "error" | "info",
        ) => {
            let message: React.ReactNode;
            if (msgOrErr instanceof Error) {
                message = msgOrErr.message;
                rsconsole.error(msgOrErr);
            } else message = msgOrErr;
            if (typeof msgOrErr === "string") {
                if (type === "error") {
                    rsconsole.trace(msgOrErr);
                } else if (type === "warning") {
                    rsconsole.warn(msgOrErr);
                } else if (type === "info") {
                    rsconsole.info(msgOrErr);
                }
            }
            try {
                showToast(message, type);
            } catch (e: any) {
                rsconsole.error(e);
            }
        },
        [rsconsole],
    );
    const ctx = useMemo(
        () => ({
            toast: fn,
        }),
        [fn],
    );

    return (
        <ToastContext.Provider value={ctx}>{children}</ToastContext.Provider>
    );
}

export default ToastProvider;
