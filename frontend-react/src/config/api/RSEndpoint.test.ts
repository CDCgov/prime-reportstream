import { getMembershipsFromToken } from "../../hooks/UseOktaMemberships";
import { OKTA_AUTH } from "../../okta";
import { RecursiveMutable } from "../../utils/UsefulTypes";
import { mockSenderAccessToken } from "../../__mocks__/OktaTokens";

import {
    getRSRequestHeaders,
    HTTPMethods,
    RSAuthenticationTypes,
    RSEndpoint,
} from "./RSEndpoint";

const testEndpointMeta = {
    path: "/path",
    methods: {
        [HTTPMethods.GET]: {} as unknown,
    },
    queryKey: "a query key",
} as const;
const testEndpoint = new RSEndpoint(testEndpointMeta);

const dynamicEndpointMeta = {
    path: "/:something/:anything",
    methods: {
        [HTTPMethods.GET]: {} as unknown,
    },
    queryKey: "anything",
} as const;
const dynamicEndpoint = new RSEndpoint(dynamicEndpointMeta);

describe("RSEndpoint", () => {
    test("instantiates with expected values", () => {
        expect(testEndpoint.meta.path).toEqual("/path");
        expect(testEndpoint.meta.queryKey).toEqual("a query key");
        expect(Object.keys(testEndpoint.fetchers)).toEqual([
            HTTPMethods.GET.toLocaleLowerCase(),
        ]);
        // not sure how to test the dynamic functions
    });

    test("has a getter for url that works", () => {
        expect(testEndpoint.url).toEqual("https://test.prime.cdc.gov/api/path");
    });

    describe("toDynamicUrl", () => {
        test("throws if endpoint has dynamic segments and no values are passed", () => {
            expect(() => dynamicEndpoint.toDynamicUrl()).toThrow();
        });

        test("returns url if no dynamic segments are present and no values are passed", () => {
            expect(testEndpoint.toDynamicUrl()).toEqual(
                "https://test.prime.cdc.gov/api/path"
            );
        });

        test("throws if not all dynamic segments are provided with values", () => {
            expect(() =>
                dynamicEndpoint.toDynamicUrl({ something: "else" })
            ).toThrow();
        });

        test("replaces all dynamic segments with provided values", () => {
            expect(
                dynamicEndpoint.toDynamicUrl({
                    something: "else",
                    anything: "more",
                })
            ).toEqual("https://test.prime.cdc.gov/api/else/more");
        });
    });

    describe("toAxiosConfig", () => {
        test("passes along key params from class", () => {
            expect(
                testEndpoint.toAxiosConfig({ method: HTTPMethods.GET })
            ).toEqual({
                url: "https://test.prime.cdc.gov/api/path",
                method: "GET",
            });
        });
        test("passes along additional options", () => {
            expect(
                testEndpoint.toAxiosConfig({
                    method: HTTPMethods.GET,
                    headers: { "x-fake-header": "anyway" },
                })
            ).toEqual({
                url: "https://test.prime.cdc.gov/api/path",
                method: "GET",
                headers: {
                    "x-fake-header": "anyway",
                },
            });
        });
        test("does not overwrite key params with options", () => {
            expect(
                testEndpoint.toAxiosConfig({
                    headers: { "x-fake-header": "anyway" },
                    url: "do not use",
                    method: HTTPMethods.GET,
                })
            ).toEqual({
                url: "https://test.prime.cdc.gov/api/path",
                method: "GET",
                headers: {
                    "x-fake-header": "anyway",
                },
            });
        });
        test("does not pass through segments data", () => {
            expect(
                dynamicEndpoint.toAxiosConfig({
                    method: HTTPMethods.GET,
                    headers: { "x-fake-header": "anyway" },
                    segments: {
                        something: "else",
                        anything: "more",
                    },
                })
            ).toEqual({
                url: "https://test.prime.cdc.gov/api/else/more",
                method: "GET",
                headers: {
                    "x-fake-header": "anyway",
                },
            });
        });
    });

    const getTokensInstance = jest.spyOn(OKTA_AUTH.tokenManager, "getTokens");
    // const axiosInstance = jest.spyOn(Axios,"default");
    // const warnInstance = jest.spyOn(console, "warn");

    const tokens = {
        accessToken: mockSenderAccessToken as unknown as RecursiveMutable<
            typeof mockSenderAccessToken
        >,
    };
    const { activeMembership } = getMembershipsFromToken(tokens.accessToken);
    // const mockResponse = Promise.resolve({foo: "bar"}) as any;

    describe("getAPIRequestHeaders", () => {
        it("authenticated", async () => {
            getTokensInstance.mockResolvedValueOnce(Promise.resolve(tokens));
            const headers = await getRSRequestHeaders();
            expect(headers["authentication-type"]).toEqual(
                RSAuthenticationTypes.OKTA
            );
            expect(headers.authorization).toEqual(
                `Bearer ${mockSenderAccessToken.accessToken}`
            );
            expect(headers.organization).toEqual(activeMembership?.parsedName);
        });

        it("unauthenticated", async () => {
            getTokensInstance.mockResolvedValueOnce({});
            const headers = await getRSRequestHeaders();
            expect(headers["authentication-type"]).toBeUndefined();
            expect(headers.authorization).toBeUndefined();
            expect(headers.organization).toBeUndefined();
        });

        it("provided headers take priority", async () => {
            getTokensInstance.mockResolvedValueOnce(Promise.resolve(tokens));
            const override = "foo";
            const headers = await getRSRequestHeaders({
                organization: override,
            });
            expect(headers["authentication-type"]).toEqual(
                RSAuthenticationTypes.OKTA
            );
            expect(headers.authorization).toEqual(
                `Bearer ${mockSenderAccessToken.accessToken}`
            );
            expect(headers.organization).toEqual(override);
        });
    });

    // TODO: Endpoint fetchers, createFetcher, queryFn
});
