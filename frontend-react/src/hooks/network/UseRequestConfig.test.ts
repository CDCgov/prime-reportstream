import { act, renderHook } from "@testing-library/react-hooks";
import { rest } from "msw";
import { setupServer } from "msw/node";

import {
    createRequestConfig,
    endpointHasMethod,
    RSRequestConfig,
} from "../../network/api/NewApi";
import { MyApi, MyApiItem } from "../../network/api/mocks/MockApi";

import useRequestConfig, {
    deletesData,
    hasData,
    needsData,
    needsTrigger,
} from "./UseRequestConfig";

const mockConsoleError = jest.spyOn(global.console, "error");

const handlers = [
    /* Returns a list of two fake api items */
    rest.get("https://test.prime.cdc.gov/api/test/test", (_req, res, ctx) => {
        return res(
            ctx.status(200),
            ctx.json([new MyApiItem("1"), new MyApiItem("2")])
        );
    }),
    /* Returns a single item by id */
    rest.get<RSRequestConfig, {}, MyApiItem | any>(
        "https://test.prime.cdc.gov/api/test/test/:id",
        (req, res, ctx) => {
            //@ts-ignore
            const { id } = req.params || "0";
            return res(ctx.status(200), ctx.json(new MyApiItem(id)));
        }
    ),

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

    test("default render state", () => {
        const getConfig = createRequestConfig<{ id: number }, MyApiItem>(
            MyApi,
            "itemById",
            "GET",
            "TOKEN",
            "ORGANIZATION",
            { id: 3 },
            { data: { testField: "3" } }
        ) as RSRequestConfig;
        const { result: getResult } = renderHook(() =>
            useRequestConfig<MyApiItem>(getConfig)
        );
        expect(getResult.current.data).toBeUndefined();
        expect(getResult.current.error).toEqual("");
        expect(getResult.current.loading).toBeTruthy(); // KEY DIFFERENCE

        const postConfig = createRequestConfig<{ id: number }, MyApiItem>(
            MyApi,
            "itemById",
            "POST",
            "TOKEN",
            "ORGANIZATION",
            { id: 3 },
            { data: { testField: "3" } }
        ) as RSRequestConfig;
        const { result: postResult } = renderHook(() =>
            useRequestConfig<MyApiItem>(postConfig)
        );
        expect(postResult.current.data).toBeUndefined();
        expect(postResult.current.error).toEqual("");
        expect(postResult.current.loading).toBeFalsy(); // KEY DIFFERENCE
    });

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
        expect(result.current.loading).toBeTruthy();
        await waitForNextUpdate();
        expect(result.current.data).toEqual([
            { testField: "1" },
            { testField: "2" },
        ]);
        expect(result.current.loading).toBeFalsy();
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
        expect(result.current.loading).toBeFalsy();
        act(() => result.current.trigger());
        expect(result.current.loading).toBeTruthy();
        await waitForNextUpdate();
        expect(result.current.data).toEqual({ testField: "3" });
        expect(result.current.loading).toBeFalsy();
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
        expect(result.current.loading).toBeFalsy();
        act(() => result.current.trigger());
        expect(result.current.loading).toBeTruthy();
        await waitForNextUpdate();
        expect(result.current.data).toEqual({ testField: "4" });
        expect(result.current.loading).toBeFalsy();
    });

    test("takes PATCH config and returns updated object", async () => {
        const config = createRequestConfig<{ id: number }, MyApiItem>(
            MyApi,
            "itemById",
            "PATCH",
            "TOKEN",
            "ORGANIZATION",
            { id: 5 },
            { data: { testField: "5" } }
        ) as RSRequestConfig;
        const { result, waitForNextUpdate } = renderHook(() =>
            useRequestConfig<MyApiItem>(config)
        );
        expect(result.current.loading).toBeFalsy();
        act(() => result.current.trigger());
        expect(result.current.loading).toBeTruthy();
        await waitForNextUpdate();
        expect(result.current.data).toEqual({ testField: "5" });
        expect(result.current.loading).toBeFalsy();
    });

    test("takes DELETE config and returns nothing", async () => {
        const config = createRequestConfig<{ id: number }, MyApiItem>(
            MyApi,
            "itemById",
            "DELETE",
            "TOKEN",
            "ORGANIZATION",
            { id: 6 }
        ) as RSRequestConfig;
        const { result, waitForNextUpdate } = renderHook(() =>
            useRequestConfig<MyApiItem>(config)
        );
        expect(result.current.loading).toBeFalsy();
        act(() => result.current.trigger());
        expect(result.current.loading).toBeTruthy();
        await waitForNextUpdate();
        expect(result.current.data).toEqual({});
        expect(result.current.loading).toBeFalsy();
    });

    test("catches server errors", async () => {
        const config = createRequestConfig<{ id: number }, MyApiItem>(
            MyApi,
            "itemById",
            "DELETE",
            "",
            "ORGANIZATION",
            { id: 7 }
        ) as RSRequestConfig;
        const { result, waitForNextUpdate } = renderHook(() =>
            useRequestConfig<MyApiItem>(config)
        );
        expect(result.current.loading).toBeFalsy();
        act(() => result.current.trigger());
        expect(result.current.loading).toBeTruthy();
        await waitForNextUpdate();
        expect(result.current.error).toEqual(
            "Request failed with status code 401"
        );
        expect(result.current.loading).toBeFalsy();
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
        expect(result.current.loading).toBeFalsy();
        expect(result.current.data).toBeUndefined();
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

test("deletesData identifies configs with DELETE method", () => {
    expect(deletesData("DELETE")).toBeTruthy();
    expect(deletesData("GET")).toBeFalsy();
});

test("needsTrigger identifies configs that are not GETs", () => {
    expect(needsTrigger("GET")).toBeFalsy();
    expect(needsTrigger("DELETE")).toBeTruthy();
});

test("endpointHasMethod verifies that the endpoint can perform a request type", () => {
    const badResult = () => endpointHasMethod(MyApi, "itemById", "OPTIONS");
    expect(badResult).toThrowError("Method OPTIONS cannot be used by itemById");
});
