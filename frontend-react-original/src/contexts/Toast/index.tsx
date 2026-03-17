import { createContext, PropsWithChildren, ReactNode, useCallback, useContext, useMemo } from "react";
import { toast } from "react-toastify";

import useSessionContext from "../Session/useSessionContext";

export interface ToastCtx {
    toast: (msgOrErr: Error | ReactNode, type: "success" | "warning" | "error" | "info") => void;
}

export const ToastContext = createContext<ToastCtx>({ toast: () => void 0 });

export const showToast = (message: ReactNode, type: "success" | "warning" | "error" | "info") => {
    // eslint-disable-next-line @typescript-eslint/no-base-to-string
    const toastId = `id_${message?.toString()}`.replace(/\W/gi, "_").substring(0, 512);
    const msg = message instanceof Error ? message.message : message;

    // basically the USWDS Alert UI
    toast(
        <div
            className={`usa-alert usa-alert--slim usa-alert--no-icon usa-alert--${type} rs-alert-toast`}
            data-testid="alerttoast"
        >
            <div className="usa-alert__body">{msg && <h2 className="usa-alert__heading">{msg}</h2>}</div>
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

function ToastProvider({ children }: PropsWithChildren) {
    const { rsConsole } = useSessionContext();
    const fn = useCallback(
        (msgOrErr: Error | ReactNode, type: "success" | "warning" | "error" | "info") => {
            let message: ReactNode;
            if (msgOrErr instanceof Error) {
                message = msgOrErr.message;
                rsConsole.error(msgOrErr);
            } else message = msgOrErr;
            if (typeof msgOrErr === "string") {
                if (type === "error") {
                    rsConsole.trace(msgOrErr);
                } else if (type === "warning") {
                    rsConsole.warn(msgOrErr);
                } else if (type === "info") {
                    rsConsole.info(msgOrErr);
                }
            }
            try {
                showToast(message, type);
            } catch (e: any) {
                rsConsole.error(e);
            }
        },
        [rsConsole],
    );
    const ctx = useMemo(
        () => ({
            toast: fn,
        }),
        [fn],
    );

    return <ToastContext.Provider value={ctx}>{children}</ToastContext.Provider>;
}

export default ToastProvider;
