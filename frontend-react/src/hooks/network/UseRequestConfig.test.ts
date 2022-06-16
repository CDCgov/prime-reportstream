import { act, renderHook } from "@testing-library/react-hooks";
import { rest } from "msw";
import { setupServer } from "msw/node";

import {
    createRequestConfig,
    endpointHasMethod,
    RSRequestConfig,
} from "../../network/api/NewApi";
import { MyApi, MyApiItem } from "../../network/api/test-tools/MockApi";

import useRequestConfig, {
    deletesData,
    hasData,
    needsData,
} from "./UseRequestConfig";

const mockConsoleError = jest.spyOn(global.console, "error");

const handlers = [
    /* Returns a list of two fake api items */
    rest.get("https://test.prime.cdc.gov/api/test/test", (req, res, ctx) => {
        return res(
            ctx.status(200),
            ctx.json([new MyApiItem("1"), new MyApiItem("2")])
        );
    }),
    /* Returns a single item by id */
    rest.post<RSRequestConfig, {}, MyApiItem | any>(
        "https://test.prime.cdc.gov/api/test/test/:id",
        (req, res, ctx) => {
            //@ts-ignore
            const { id } = req.params || "0";
            return res(ctx.status(200), ctx.json(new MyApiItem(id)));
        }
    ),

    rest.put<RSRequestConfig, {}, MyApiItem>(
        "https://test.prime.cdc.gov/api/test/test/:id",
        (req, res, ctx) => {
            //@ts-ignore
            const { id } = req.params || "0";
            return res(ctx.status(200), ctx.json(new MyApiItem(id)));
        }
    ),

    rest.patch<RSRequestConfig, {}, MyApiItem>(
        "https://test.prime.cdc.gov/api/test/test/:id",
        (req, res, ctx) => {
            //@ts-ignore
            const { id } = req.params || "0";
            return res(ctx.status(200), ctx.json(new MyApiItem(id)));
        }
    ),

    rest.delete<RSRequestConfig, {}, MyApiItem | any>(
        "https://test.prime.cdc.gov/api/test/test/:id",
        (req, res, ctx) => {
            if (!req.headers.get("authorization")?.includes("TOKEN")) {
                return res(ctx.status(401));
            } else {
                return res(ctx.status(200));
            }
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
        ) as RSRequestConfig;
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
        ) as RSRequestConfig;
        const { result, waitForNextUpdate } = renderHook(() =>
            useRequestConfig<MyApiItem>(config)
        );
        act(() => result.current.trigger());
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
        ) as RSRequestConfig;
        const { result, waitForNextUpdate } = renderHook(() =>
            useRequestConfig<MyApiItem>(config)
        );
        act(() => result.current.trigger());
        await waitForNextUpdate();
        expect(result.current.data).toEqual({ testField: "4" });
    });

    test("takes PATCH config and returns updated object", async () => {
        const config = createRequestConfig<{ id: number }, MyApiItem>(
            MyApi,
            "itemById",
            "PATCH",
            "TOKEN",
            "ORGANIZATION",
            { id: 4 },
            { data: { testField: "4" } }
        ) as RSRequestConfig;
        const { result, waitForNextUpdate } = renderHook(() =>
            useRequestConfig<MyApiItem>(config)
        );
        act(() => result.current.trigger());
        await waitForNextUpdate();
        expect(result.current.data).toEqual({ testField: "4" });
    });

    test("takes DELETE config and returns nothing", async () => {
        const config = createRequestConfig<{ id: number }, MyApiItem>(
            MyApi,
            "itemById",
            "DELETE",
            "TOKEN",
            "ORGANIZATION",
            { id: 4 }
        ) as RSRequestConfig;
        const { result, waitForNextUpdate } = renderHook(() =>
            useRequestConfig<MyApiItem>(config)
        );
        act(() => result.current.trigger());
        await waitForNextUpdate();
        expect(result.current.data).toEqual(undefined);
    });

    test("catches server errors", async () => {
        const config = createRequestConfig<{ id: number }, MyApiItem>(
            MyApi,
            "itemById",
            "DELETE",
            "",
            "ORGANIZATION",
            { id: 4 }
        ) as RSRequestConfig;
        const { result, waitForNextUpdate } = renderHook(() =>
            useRequestConfig<MyApiItem>(config)
        );
        act(() => result.current.trigger());
        await waitForNextUpdate();
        expect(result.current.error).toEqual(
            "Request failed with status code 401"
        );
    });

    test("catches local errors", async () => {
        const config = createRequestConfig<{ id: number }, MyApiItem>(
            MyApi,
            "itemById",
            "POST",
            "",
            "ORGANIZATION",
            { id: 4 }
        ) as RSRequestConfig;
        const { result } = renderHook(() =>
            useRequestConfig<MyApiItem>(config)
        );
        expect(result.current.data).toBeUndefined();
        expect(result.current.loading).toBeFalsy();
        expect(result.current.error).toEqual(
            "This call requires data to be passed in"
        );
        expect(mockConsoleError).toHaveBeenCalledWith(
            "This call requires data to be passed in"
        );
    });
});

test("needsData", () => {
    expect(needsData("GET")).toBeFalsy();
    expect(needsData("POST")).toBeTruthy();
});

test("hasData", () => {
    const config = createRequestConfig<{ id: number }, MyApiItem>(
        MyApi,
        "itemById",
        "POST",
        "TOKEN",
        "ORGANIZATION",
        { id: 4 }
    ) as RSRequestConfig;
    expect(hasData(config)).toBeFalsy();
    config.data = new MyApiItem("4");
    expect(hasData(config)).toBeTruthy();
});

test("deletesData", () => {
    expect(deletesData("DELETE")).toBeTruthy();
    expect(deletesData("GET")).toBeFalsy();
});

test("endpointHasMethod", () => {
    const result = endpointHasMethod(MyApi, "itemById", "GET");
    expect(result).toBeTruthy();
    const badResult = () => endpointHasMethod(MyApi, "itemById", "OPTIONS");
    expect(badResult).toThrowError("Method OPTIONS cannot be used by itemById");
});
