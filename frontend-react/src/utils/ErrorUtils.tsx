import { Dispatch, SetStateAction } from "react";

import { RSConsole } from "./console";

export interface ReportStreamAlert {
    type: string;
    message: string;
}

// does a few things with an error:
// - logs a message if passed
// - parses out the most useful error message to display from the error
// - sets state using the passed function to alert via the ui in whatever
// way the implementing component sees fit
export const handleErrorWithAlert = ({
    error,
    logMessage,
    setAlert,
    rsconsole,
}: {
    error: any;
    logMessage?: string;
    setAlert?: Dispatch<SetStateAction<ReportStreamAlert | undefined>>;
    rsconsole: RSConsole;
}) => {
    if (error) {
        rsconsole.error(error);
    }
    if (logMessage) {
        rsconsole.warn(logMessage);
    }
    // attempt to extract more helpful error from response
    const { response: { data: { error: errorString = null } = {} } = {} } =
        error;
    const message = errorString || error.toString();
    if (setAlert) {
        setAlert({ type: "error", message });
    }
};
