/** For consistency, when passing the code prop, please use these values
 * e.g. <ErrorComponent code={RSError.NOT_FOUND} /> */
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

/** Throw from any failing network calls, and pass in the status code to
 * match it with the right display */
export class RSNetworkError extends Error {
    /* Used for identifying unique content to display for error */
    code: ErrorName;
    /* Used to determine if this error should render as a message or full page */
    displayAsPage: boolean = false;
    /* Build a new RSNetworkError */
    constructor(message: string, status?: number, displayAsPage?: boolean) {
        super(message); // Sets message
        this.code = this.parseStatus(status); // Sets code using child's parseStatus
        if (displayAsPage !== undefined) this.displayAsPage = displayAsPage; // Updates display from default if present
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
export const isRSNetworkError = (e: object) => e instanceof RSNetworkError;
