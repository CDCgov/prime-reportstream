import { Dispatch, SetStateAction } from "react";

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
}: {
    error: any;
    logMessage?: string;
    setAlert?: Dispatch<SetStateAction<ReportStreamAlert | undefined>>;
}) => {
    if (logMessage) {
        console.error(logMessage);
    }
    // attempt to extract more helpful error from response
    const { response: { data: { error: errorString = null } = {} } = {} } =
        error;
    const message = errorString || error.toString();
    if (setAlert) {
        setAlert({ type: "error", message });
    }
};

/* Useful for when a function catches errors and could possibly
 * return something other than the desired object type
 *
 * example: MyObjectError extends SimpleError { ... }
 * example: a return type of MyObject | SimpleError  */
export class SimpleError {
    message: string;
    constructor(message: string) {
        this.message = message;
    }
}
