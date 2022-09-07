import { ErrorName } from "../components/RSErrorBoundary";

export type ErrorUI = "message" | "page";

/** All custom errors should extend this class */
export class RSError extends Error {
    code: ErrorName;
    displayAs: ErrorUI = "page";

    /** @param message {string} Passed to `Error.constructor()`
     * @param status {number?} Used to parse `RSError.code` value
     * @param display {ErrorUI} Used to display as either a message or page */
    constructor(message: string, status?: number, display?: ErrorUI) {
        super(message);
        this.code = this.parseStatus(status);
        if (display) this.displayAs = display;
    }
    /** Feed it a status, and get back the proper enumerated name */
    parseStatus(status?: number) {
        if (status === undefined) return ErrorName.UNKNOWN;
        switch (status) {
            case 401:
                return ErrorName.UNAUTHORIZED;
            case 404:
                return ErrorName.NOT_FOUND;
            default:
                return ErrorName.UNKNOWN;
        }
    }
}
/** Throw from any failing network calls, and pass in the status code to
 * match it with the right RSErrorPage */
export class RSNetworkError extends RSError {
    constructor(message: string, status?: number, display?: ErrorUI) {
        super(message, status, display);
        Object.setPrototypeOf(this, RSNetworkError.prototype);
    }
}
/** Easy boolean check to validate error is of RSError descent */
export const isRSError = (e: object) => e instanceof RSError;
