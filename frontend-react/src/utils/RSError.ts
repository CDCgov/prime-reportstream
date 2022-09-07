import { ErrorName } from "../components/RSErrorBoundary";

/** All custom errors should extend this class */
export class RSError extends Error {
    code: ErrorName;

    constructor(message: string, status?: number) {
        super(message);
        this.code = this.parseStatus(status);
    }

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

export class RSNetworkError extends RSError {
    constructor(message: string, status?: number) {
        super(message, status);
        Object.setPrototypeOf(this, RSNetworkError.prototype);
    }
}

export const isRSError = (e: any) => e instanceof RSError;
