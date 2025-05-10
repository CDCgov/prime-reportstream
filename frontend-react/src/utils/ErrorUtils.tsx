import { Dispatch, SetStateAction } from "react";

import { RSConsole } from "./rsConsole/rsConsole";

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
    rsConsole,
}: {
    error: any;
    logMessage?: string;
    setAlert?: Dispatch<SetStateAction<ReportStreamAlert | undefined>>;
    rsConsole: RSConsole;
}) => {
    if (error) {
        rsConsole.error(error);
    }
    if (logMessage) {
        rsConsole.warn(logMessage);
    }
    // attempt to extract more helpful error from response
    const { response: { data: { error: errorString = null } = {} } = {} } = error;
    const message = errorString ?? error.toString();
    if (setAlert) {
        setAlert({ type: "error", message });
    }
};
