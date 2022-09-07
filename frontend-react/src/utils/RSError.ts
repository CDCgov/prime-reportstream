import { ErrorName } from "../components/RSErrorBoundary";

/** All custom errors should extend this class */
export class RSError extends Error {
    code: ErrorName;

    /** @param message {string} Passed to `Error.constructor()`
     * @param status {number?} Used to parse `RSError.code` value */
    constructor(message: string, status?: number) {
        super(message);
        this.code = this.parseStatus(status);
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
    constructor(message: string, status?: number) {
        super(message, status);
        Object.setPrototypeOf(this, RSNetworkError.prototype);
    }
}
/** Easy boolean check to validate error is of RSError descent */
export const isRSError = (e: any) => e instanceof RSError;
