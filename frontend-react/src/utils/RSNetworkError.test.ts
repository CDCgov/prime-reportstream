import { AxiosError, AxiosResponse } from "axios";

import { ErrorName, RSNetworkError } from "./RSNetworkError";

const DEFAULT_MSG = "I require a message";

describe("RSNetworkError", () => {
    describe("constructor", () => {
        test("handles positive case", () => {
            const axiosError = new AxiosError(
                DEFAULT_MSG,
                "400",
                undefined,
                {},
                {
                    status: 400,
                    data: {
                        isThere: true,
                    },
                } as AxiosResponse,
            );
            const error = new RSNetworkError(axiosError);
            expect(error.name).toEqual(ErrorName.BAD_REQUEST);
            expect(error.message).toEqual(DEFAULT_MSG);
            expect(error.data).toEqual({ isThere: true });
            expect(error.originalError).toEqual(axiosError);
        });
        test("works with incomplete response", () => {
            const axiosError = new AxiosError(
                DEFAULT_MSG,
                undefined,
                undefined,
                {},
                {} as AxiosResponse,
            );
            const error = new RSNetworkError(axiosError);
            expect(error.name).toEqual(ErrorName.UNKNOWN);
            expect(error.message).toEqual(DEFAULT_MSG);
            expect(error.data).toEqual(undefined);
            expect(error.originalError).toEqual(axiosError);
        });
    });
    describe("parseStatus", () => {
        test("Bad Request", () => {
            const axiosError = new AxiosError(
                DEFAULT_MSG,
                "400",
                undefined,
                {},
                {
                    status: 400,
                } as AxiosResponse,
            );
            const error = new RSNetworkError(axiosError);
            expect(error.name).toEqual(ErrorName.BAD_REQUEST);
            expect(error.originalError).toEqual(axiosError);
        });
        test("Unauthorized", () => {
            const axiosError = new AxiosError(
                DEFAULT_MSG,
                "401",
                undefined,
                {},
                {
                    status: 401,
                } as AxiosResponse,
            );
            const error = new RSNetworkError(axiosError);
            expect(error.name).toEqual(ErrorName.UNAUTHORIZED);
            expect(error.originalError).toEqual(axiosError);
        });
        test("Not Found", () => {
            const axiosError = new AxiosError(
                DEFAULT_MSG,
                "404",
                undefined,
                {},
                {
                    status: 404,
                } as AxiosResponse,
            );
            const error = new RSNetworkError(axiosError);
            expect(error.name).toEqual(ErrorName.NOT_FOUND);
            expect(error.originalError).toEqual(axiosError);
        });
        test("Server Error", () => {
            const axiosError = new AxiosError(
                DEFAULT_MSG,
                "503",
                undefined,
                {},
                {
                    status: 503,
                } as AxiosResponse,
            );
            const error = new RSNetworkError(axiosError);
            expect(error.name).toEqual(ErrorName.SERVER_ERROR);
            expect(error.originalError).toEqual(axiosError);
        });
        test("Catch-all (Unknown Error)", () => {
            const axiosError = new AxiosError(
                DEFAULT_MSG,
                "467",
                undefined,
                {},
                {
                    status: 467,
                } as AxiosResponse,
            );
            const error = new RSNetworkError(axiosError);
            expect(error.name).toEqual(ErrorName.UNKNOWN);
            expect(error.originalError).toEqual(axiosError);
        });
        test("Catch-all (Undefined Status)", () => {
            const axiosError = new AxiosError(
                DEFAULT_MSG,
                undefined,
                undefined,
                undefined,
                undefined,
            );
            const error = new RSNetworkError(axiosError);
            expect(error.name).toEqual(ErrorName.UNKNOWN);
            expect(error.originalError).toEqual(axiosError);
        });
    });
});
