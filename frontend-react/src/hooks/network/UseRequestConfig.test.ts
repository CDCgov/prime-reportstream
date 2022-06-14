import { renderHook } from "@testing-library/react-hooks";
import { rest } from "msw";
import { setupServer } from "msw/node";

import {
    API,
    ApiBaseUrls,
    createRequestConfig,
    RSRequestConfig,
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

MyApi.endpoints.set("itemById", {
    url: "/test/:id",
    methods: ["POST", "PUT", "PATCH", "DELETE"],
});

const handlers = [
    /* Returns a list of two fake api items */
    rest.get("https://test.prime.cdc.gov/api/test/test", (req, res, ctx) => {
        return res(
            ctx.status(200),
            ctx.json([makeApiItem("1"), makeApiItem("2")])
        );
    }),
    /* Returns a single item by id */
    rest.post<RSRequestConfig, {}, MyApiItem>(
        "https://test.prime.cdc.gov/api/test/test/:id",
        (req, res, ctx) => {
            if (!req.headers.get("authorization")?.includes("TOKEN")) {
                return res(ctx.status(401));
            } else {
                //@ts-ignore
                const { id } = req.params || "0";
                return res(ctx.status(200), ctx.json(makeApiItem(id)));
            }
        }
    ),

    rest.put<RSRequestConfig, {}, MyApiItem>(
        "https://test.prime.cdc.gov/api/test/test/:id",
        (req, res, ctx) => {
            if (!req.headers.get("authorization")?.includes("TOKEN")) {
                return res(ctx.status(401));
            } else {
                //@ts-ignore
                const { id } = req.params || "0";
                return res(ctx.status(200), ctx.json(makeApiItem(id)));
            }
        }
    ),

    rest.patch<RSRequestConfig, {}, MyApiItem>(
        "https://test.prime.cdc.gov/api/test/test/:id",
        (req, res, ctx) => {
            if (!req.headers.get("authorization")?.includes("TOKEN")) {
                return res(ctx.status(401));
            } else {
                //@ts-ignore
                const { id } = req.params || "0";
                return res(ctx.status(200), ctx.json(makeApiItem(id)));
            }
        }
    ),

    rest.delete<RSRequestConfig, {}, MyApiItem>(
        "https://test.prime.cdc.gov/api/test/test/:id",
        (req, res, ctx) => {
            return res(ctx.status(200));
        }
    ),
];

/* TEST SERVER TO USE IN `.test.ts` FILES */
const testServer = setupServer(...handlers);

describe("useRequestConfig", () => {
    beforeAll(() => testServer.listen());
    afterEach(() => testServer.resetHandlers());
    afterAll(() => testServer.close());
    test("takes GET config and fetches results", async () => {
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

    test("takes POST config and returns created object", async () => {
        const config = createRequestConfig<{ id: number }, MyApiItem>(
            MyApi,
            "itemById",
            "POST",
            "TOKEN",
            "ORGANIZATION",
            { id: 3 },
            { data: { testField: "3" } }
        );
        const { result, waitForNextUpdate } = renderHook(() =>
            useRequestConfig<MyApiItem>(config)
        );
        await waitForNextUpdate();
        expect(result.current.data).toEqual({ testField: "3" });
    });

    test("takes PUT config and returns created object", async () => {
        const config = createRequestConfig<{ id: number }, MyApiItem>(
            MyApi,
            "itemById",
            "PUT",
            "TOKEN",
            "ORGANIZATION",
            { id: 4 },
            { data: { testField: "4" } }
        );
        const { result, waitForNextUpdate } = renderHook(() =>
            useRequestConfig<MyApiItem>(config)
        );
        await waitForNextUpdate();
        expect(result.current.data).toEqual({ testField: "4" });
    });

    test("takes PATCH config and returns created object", async () => {
        const config = createRequestConfig<{ id: number }, MyApiItem>(
            MyApi,
            "itemById",
            "PATCH",
            "TOKEN",
            "ORGANIZATION",
            { id: 4 },
            { data: { testField: "4" } }
        );
        const { result, waitForNextUpdate } = renderHook(() =>
            useRequestConfig<MyApiItem>(config)
        );
        await waitForNextUpdate();
        expect(result.current.data).toEqual({ testField: "4" });
    });

    test("takes PATCH config and returns created object", async () => {
        const config = createRequestConfig<{ id: number }, MyApiItem>(
            MyApi,
            "itemById",
            "DELETE",
            "TOKEN",
            "ORGANIZATION",
            { id: 4 }
        );
        const { result, waitForNextUpdate } = renderHook(() =>
            useRequestConfig<MyApiItem>(config)
        );
        await waitForNextUpdate();
        expect(result.current.data).toEqual(undefined);
    });
});
