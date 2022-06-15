import {
    API,
    ApiBaseUrls,
    buildEndpointUrl,
    createAxiosConfig,
} from "./NewApi";

const mockConsoleWarn = jest.spyOn(global.console, "warn");
const mockConsoleError = jest.spyOn(global.console, "error");
const MyApi: API = {
    baseUrl: ApiBaseUrls.HISTORY,
    endpoints: new Map(),
};

MyApi.endpoints.set("list", {
    url: "/test",
    methods: ["GET"],
});
MyApi.endpoints.set("detail", {
    url: "/test/:id",
    methods: ["GET", "PUT"],
});

describe("Api interfaces", () => {
    test("buildEndpointUrl: happy path", () => {
        /* URL without parameters */
        const listURL = buildEndpointUrl(MyApi, "list");
        expect(listURL).toEqual("https://test.prime.cdc.gov/api/history/test");

        /* URL with parameters */
        const detailUrlWithId = buildEndpointUrl<{ id: number }>(
            MyApi,
            "detail",
            { id: 123 }
        );
        expect(detailUrlWithId).toEqual(
            "https://test.prime.cdc.gov/api/history/test/123"
        );
    });

    test("buildEndpointUrl: defaults on bad input", () => {
        /* URL with bad params */
        const detailUrlWithoutId = buildEndpointUrl<{ idSpelledWrong: number }>(
            MyApi,
            "detail",
            { idSpelledWrong: 123 }
        );
        expect(detailUrlWithoutId).toEqual(
            "https://test.prime.cdc.gov/api/history/test/:id"
        );
    });

    test("buildEndpointUrl: invalid endpoint key", () => {
        /* Endpoint does not exist */
        buildEndpointUrl<{ id: number }>(MyApi, "detailSpelledWrong", {
            id: 123,
        });
        expect(mockConsoleError).toHaveBeenCalledWith(
            "You must provide a valid endpoint key: detailSpelledWrong not found"
        );
    });

    test("createAxiosConfig: basic config", () => {
        const baseConfig = createAxiosConfig(MyApi, "list", "GET");
        expect(baseConfig).toEqual({
            url: "https://test.prime.cdc.gov/api/history/test",
            method: "GET",
            headers: {
                "authentication-type": "okta",
                authorization: "Bearer ",
                organization: "",
            },
        });
    });

    test("createAxiosConfig: with auth and params", () => {
        const configWithAuth = createAxiosConfig<{ id: number }>(
            MyApi,
            "detail",
            "GET",
            "TOKEN",
            "ORGANIZATION",
            { id: 123 }
        );
        expect(configWithAuth).toEqual({
            url: "https://test.prime.cdc.gov/api/history/test/123",
            method: "GET",
            headers: {
                "authentication-type": "okta",
                authorization: "Bearer TOKEN",
                organization: "ORGANIZATION",
            },
        });
    });

    test("createAxiosConfig: url didn't parse", () => {
        mockConsoleWarn.mockReturnValue();

        createAxiosConfig(MyApi, "detail", "GET", "TOKEN", "ORGANIZATION");

        expect(mockConsoleWarn).toHaveBeenCalledWith(
            "Looks like your url didn't parse!"
        );
        expect(mockConsoleError).toHaveBeenCalledWith(
            "Parameters are required for detail: /test/:id"
        );
    });
});
