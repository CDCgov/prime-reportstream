export type ErrorUI = "message" | "page";
/** For consistency, when passing the code prop, please use these values
 * e.g. <ErrorPage code={RSError.NOT_FOUND} /> */
export enum ErrorName {
    // TODO: Update App.tsx to throw on bad browser, wrap with boundary in index.ts?
    UNSUPPORTED_BROWSER = "unsupported-browser",
    UNAUTHORIZED = "unauthorized",
    NOT_FOUND = "not-found",
    // Any error thrown that cannot be parsed by RSError.parseStatus()
    UNKNOWN = "unknown-error",
    // Any error that does not extend the RSError class
    NON_RS_ERROR = "non-rs-error",
}

/** All custom errors should extend this class */
export abstract class RSError extends Error {
    /* Used for identifying unique content to display for error */
    code: ErrorName;
    /* Used to determine if this error should render as a message or full page */
    displayAs: ErrorUI = "page";

    /** @param message {string} Passed to `Error.constructor()`
     * @param status {number?} Used to parse `RSError.code` value
     * @param display {ErrorUI} Used to display as either a message or page */
    protected constructor(message: string, status?: number, display?: ErrorUI) {
        super(message); // Sets message
        this.code = this.parseStatus(status); // Sets code using child's parseStatus
        if (display) this.displayAs = display; // Updates display from default if present
    }
    /** Used to turn a status into an {@link ErrorName} */
    abstract parseStatus(status?: any): ErrorName;
}
/** Throw from any failing network calls, and pass in the status code to
 * match it with the right RSErrorPage */
export class RSNetworkError extends RSError {
    constructor(message: string, status?: number, display?: ErrorUI) {
        super(message, status, display);
        Object.setPrototypeOf(this, RSNetworkError.prototype);
    }
    /** Map response status code to error name */
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
/** Easy boolean check to validate error is of RSError descent */
export const isRSError = (e: object) => e instanceof RSError;
