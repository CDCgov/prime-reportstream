import { renderHook } from "@testing-library/react-hooks";
import { rest } from "msw";
import { setupServer } from "msw/node";

import {
    API,
    ApiBaseUrls,
    createRequestConfig,
} from "../../network/api/NewApi";

import useRequestConfig from "./UseRequestConfig";

interface MyApiItem {
    testField: string;
}
const makeApiItem = (s: string) => ({ testField: s });
const MyApi: API = {
    baseUrl: ApiBaseUrls.TEST,
    endpoints: new Map(),
};

MyApi.endpoints.set("list", {
    url: "/test",
    methods: ["GET"],
});

const handlers = [
    /* Successfully returns a Report */
    rest.get("https://test.prime.cdc.gov/api/test/test", (req, res, ctx) => {
        return res(
            ctx.status(200),
            ctx.json([makeApiItem("1"), makeApiItem("2")])
        );
    }),
];

/* TEST SERVER TO USE IN `.test.ts` FILES */
const testServer = setupServer(...handlers);

describe("useRequestConfig", () => {
    beforeAll(() => testServer.listen());
    afterEach(() => testServer.resetHandlers());
    afterAll(() => testServer.close());
    test("takes config and fetches results", async () => {
        const config = createRequestConfig(
            MyApi,
            "list",
            "GET",
            "TOKEN",
            "ORGANIZATION"
        );
        const { result, waitForNextUpdate } = renderHook(() =>
            useRequestConfig<MyApiItem[]>(config)
        );
        await waitForNextUpdate();
        expect(result.current.data).toEqual([
            { testField: "1" },
            { testField: "2" },
        ]);
    });
});
