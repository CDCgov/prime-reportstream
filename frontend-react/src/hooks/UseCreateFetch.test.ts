import { renderHook } from "@testing-library/react";
import * as axios from "axios";

import config from "../config";
import { RSEndpoint } from "../config/endpoints";
import { mockToken } from "../utils/TestUtils";

import * as UseCreateFetch from "./UseCreateFetch";
import { MemberType } from "./UseOktaMemberships";

let generatorSpy: jest.SpyInstance;

// a possible pattern for mocking axios in the future, which is difficult
jest.mock("axios");
const mockedAxios = jest.mocked(axios);

jest.mock("../TelemetryService", () => ({
    getAppInsightsHeaders: () => ({
        "x-ms-session-id": "DUMMY",
    }),
}));

const fakeOktaToken = mockToken();
const fakeMembership = {
    parsedName: "any",
    memberType: MemberType.SENDER,
};

const fakeEndpoint = new RSEndpoint({
    path: "/anything",
    method: "GET",
});

describe("useCreateFetch", () => {
    // This spy based mock DOES NOT WORK unless reinstantiated for each test as shown here
    beforeEach(() => {
        generatorSpy = jest.spyOn(
            UseCreateFetch.auxExports,
            "createTypeWrapperForAuthorizedFetch",
        );
    });
    afterAll(() => {
        generatorSpy.mockRestore();
    });

    test("returns a function that calls createWrapperForAuthorizedFetch with auth params", () => {
        const oktaToken = mockToken();
        const activeMembership = {
            parsedName: "any",
            memberType: MemberType.SENDER,
        };
        const { result } = renderHook(() =>
            UseCreateFetch.useCreateFetch(fakeOktaToken, fakeMembership),
        );
        expect(result.current).toBeInstanceOf(Function);
        expect(generatorSpy).not.toHaveBeenCalled();

        const invocationValue = result.current();
        expect(invocationValue).toBeInstanceOf(Function);
        expect(generatorSpy).toHaveBeenCalledWith(oktaToken, activeMembership);
    });
});

describe("createTypeWrapperForAuthorizedFetch", () => {
    const {
        auxExports: { createTypeWrapperForAuthorizedFetch },
    } = UseCreateFetch;

    beforeEach(() => {
        mockedAxios.default.mockImplementation(
            () => Promise.resolve({ data: "any data" }) as axios.AxiosPromise,
        );
    });

    test("returns an async function", () => {
        const authorizedFetch = createTypeWrapperForAuthorizedFetch(
            fakeOktaToken,
            fakeMembership,
        );
        const authorizedFetchResult = authorizedFetch(fakeEndpoint);
        expect(authorizedFetchResult).toBeInstanceOf(Promise);
    });

    test("returns a function that returns a function that calls axios with expected arguments", async () => {
        await createTypeWrapperForAuthorizedFetch(
            mockToken({ accessToken: "this token" }),
            fakeMembership,
        )(fakeEndpoint, {
            data: "some data",
            timeout: 1,
        });
        expect(axios).toHaveBeenCalledTimes(1);
        expect(axios).toHaveBeenCalledWith({
            url: `${config.API_ROOT}/anything`,
            method: "GET",
            data: "some data",
            timeout: 1,
            headers: {
                "authentication-type": "okta",
                authorization: "Bearer this token",
                organization: "any",
                "x-ms-session-id": "DUMMY",
            },
        });
    });

    test("returns a function that returns a function that calls axios with expected arguments (custom headers)", async () => {
        await createTypeWrapperForAuthorizedFetch(
            mockToken({ accessToken: "this token" }),
            fakeMembership,
        )(fakeEndpoint, {
            data: "some data",
            timeout: 1,
            headers: {
                "authentication-type": "overridden",
                "x-fake-header": "me",
            },
        });
        expect(axios).toHaveBeenCalledTimes(1);
        expect(axios).toHaveBeenCalledWith({
            url: `${config.API_ROOT}/anything`,
            method: "GET",
            data: "some data",
            timeout: 1,
            headers: {
                "authentication-type": "overridden",
                "x-fake-header": "me",
                authorization: "Bearer this token",
                organization: "any",
                "x-ms-session-id": "DUMMY",
            },
        });
    });

    test("returns a function that returns a function that calls axios with expected arguments and does not override key options", async () => {
        await createTypeWrapperForAuthorizedFetch(
            mockToken({ accessToken: "this token" }),
            fakeMembership,
        )(fakeEndpoint, {
            url: "do not use me",
            method: "POST",
            data: "some data",
            timeout: 1,
        });
        expect(axios).toHaveBeenCalledTimes(1);
        expect(axios).toHaveBeenCalledWith({
            url: `${config.API_ROOT}/anything`,
            method: "GET",
            data: "some data",
            timeout: 1,
            headers: {
                "authentication-type": "okta",
                authorization: "Bearer this token",
                organization: "any",
                "x-ms-session-id": "DUMMY",
            },
        });
    });

    test("returns a function that returns a function that returns the data object from axios response", async () => {
        const dataResult = await createTypeWrapperForAuthorizedFetch(
            mockToken({ accessToken: "this token" }),
            fakeMembership,
        )(fakeEndpoint, {
            data: "some data",
            timeout: 1,
        });
        expect(dataResult).toEqual("any data");
    });
});
