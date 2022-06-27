import { buildEndpointUrl, createRequestConfig } from "./NewApi";
import { MyApi } from "./mocks/MockApi";

jest.spyOn(global.console, "warn");
jest.spyOn(global.console, "error");

describe("Api interfaces", () => {
    test("buildEndpointUrl: happy path", () => {
        /* URL without parameters */
        const listURL = buildEndpointUrl(MyApi, "list");
        expect(listURL).toEqual("https://test.prime.cdc.gov/api/test/test");

        /* URL with parameters */
        const detailUrlWithId = buildEndpointUrl<{ id: number }>(
            MyApi,
            "itemById",
            { id: 123 }
        );
        expect(detailUrlWithId).toEqual(
            "https://test.prime.cdc.gov/api/test/test/123"
        );
    });

    test("buildEndpointUrl: defaults on bad input", () => {
        /* URL with bad params */
        const detailUrlWithoutId = buildEndpointUrl<{ idSpelledWrong: number }>(
            MyApi,
            "itemById",
            { idSpelledWrong: 123 }
        );
        expect(detailUrlWithoutId).toEqual(
            "https://test.prime.cdc.gov/api/test/test/:id"
        );
    });

    test("buildEndpointUrl: invalid endpoint key", () => {
        /* Endpoint does not exist */
        try {
            buildEndpointUrl<{ id: number }>(MyApi, "itemByIdSpelledWrong", {
                id: 123,
            });
        } catch (e: any) {
            expect(e.message).toEqual(
                "You must provide a valid endpoint key: itemByIdSpelledWrong not found"
            );
        }
    });

    test("createAxiosConfig: basic config", () => {
        const baseConfig = createRequestConfig(MyApi, "list", "GET");
        expect(baseConfig).toEqual({
            url: "https://test.prime.cdc.gov/api/test/test",
            method: "GET",
            headers: {
                "authentication-type": "okta",
                authorization: "Bearer ",
                organization: "",
            },
        });
    });

    test("createAxiosConfig: with auth and params", () => {
        const configWithAuth = createRequestConfig<{ id: number }>(
            MyApi,
            "itemById",
            "GET",
            "TOKEN",
            "ORGANIZATION",
            { id: 123 }
        );
        expect(configWithAuth).toEqual({
            url: "https://test.prime.cdc.gov/api/test/test/123",
            method: "GET",
            headers: {
                "authentication-type": "okta",
                authorization: "Bearer TOKEN",
                organization: "ORGANIZATION",
            },
        });
    });

    test("createAxiosConfig: url didn't parse", () => {
        try {
            createRequestConfig(
                MyApi,
                "itemById",
                "GET",
                "TOKEN",
                "ORGANIZATION"
            );
        } catch (e: any) {
            expect(e.message).toEqual(
                "Parameters are required for itemById: /test/:id"
            );
        }
    });
});
