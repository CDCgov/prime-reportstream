import { AxiosResponse } from "axios";

import { ErrorName, RSNetworkError } from "./RSNetworkError";

const DEFAULT_MSG = "I require a message";

describe("RSNetworkError", () => {
    describe("constructor", () => {
        test("handles positive case", () => {
            const error = new RSNetworkError(DEFAULT_MSG, {
                status: 400,
                data: {
                    isThere: true,
                },
            } as AxiosResponse);
            expect(error.name).toEqual(ErrorName.BAD_REQUEST);
            expect(error.message).toEqual(DEFAULT_MSG);
            expect(error.data).toEqual({ isThere: true });
        });
        test("works with incomplete response", () => {
            const error = new RSNetworkError(DEFAULT_MSG, undefined);
            expect(error.name).toEqual(ErrorName.UNKNOWN);
            expect(error.message).toEqual(DEFAULT_MSG);
            expect(error.data).toEqual(undefined);
        });
    });
    describe("parseStatus", () => {
        test("Bad Request", () => {
            const error = new RSNetworkError(DEFAULT_MSG, {
                status: 400,
            } as AxiosResponse);
            expect(error.name).toEqual(ErrorName.BAD_REQUEST);
        });
        test("Unauthorized", () => {
            const error = new RSNetworkError(DEFAULT_MSG, {
                status: 401,
            } as AxiosResponse);
            expect(error.name).toEqual(ErrorName.UNAUTHORIZED);
        });
        test("Not Found", () => {
            const error = new RSNetworkError(DEFAULT_MSG, {
                status: 404,
            } as AxiosResponse);
            expect(error.name).toEqual(ErrorName.NOT_FOUND);
        });
        test("Server Error", () => {
            const error = new RSNetworkError(DEFAULT_MSG, {
                status: 503,
            } as AxiosResponse);
            expect(error.name).toEqual(ErrorName.SERVER_ERROR);
        });
        test("Catch-all (Unknown Error)", () => {
            const error = new RSNetworkError(DEFAULT_MSG, {
                status: 467,
            } as AxiosResponse);
            expect(error.name).toEqual(ErrorName.UNKNOWN);
        });
        test("Catch-all (Undefined Status)", () => {
            const error = new RSNetworkError(DEFAULT_MSG, undefined);
            expect(error.name).toEqual(ErrorName.UNKNOWN);
        });
    });
});
