import { act, renderHook } from "@testing-library/react-hooks";

import { sampleServer } from "../../__mocks__/SampleServer";
import { sampleApi, SampleObject } from "../_sample/SampleApi";

import { useEndpoint } from "./UseEndpoint";

const obj = new SampleObject("string", true, 123);

describe("UseEndpoint.ts", () => {
    /* Handles setup, refresh, and closing of mock service worker */
    beforeAll(() => sampleServer.listen());
    afterEach(() => sampleServer.resetHandlers());
    afterAll(() => sampleServer.close());

    test("returns data from get call", async () => {
        const { result, waitForNextUpdate } = renderHook(() =>
            useEndpoint<SampleObject[]>(sampleApi.getSampleList())
        );
        act(() => result.current.call());
        await waitForNextUpdate();

        expect(result.current.response.loading).toBeFalsy();
        expect(result.current.response.status).toBe(200);
        expect(result.current.response.message).toBe("");
        expect(result.current.response.data).toEqual([obj, obj]);
    });

    test("posts data from post call", async () => {
        const { result, waitForNextUpdate } = renderHook(() =>
            useEndpoint<SampleObject>(sampleApi.postSampleItem(obj))
        );
        act(() => result.current.call());
        await waitForNextUpdate();

        expect(result.current.response.loading).toBeFalsy();
        expect(result.current.response.status).toBe(202);
        expect(result.current.response.message).toBe("");
        expect(result.current.response.data).toEqual(obj);
    });

    test("patches data with new values", async () => {
        const { result, waitForNextUpdate } = renderHook(() =>
            useEndpoint<Partial<SampleObject>>(
                sampleApi.patchSampleItem(123, {
                    bool: false,
                })
            )
        );
        act(() => result.current.call());
        await waitForNextUpdate();

        expect(result.current.response.loading).toBeFalsy();
        expect(result.current.response.status).toBe(202);
        expect(result.current.response.message).toBe("");
        expect(result.current.response.data).toEqual({
            received: {
                bool: false,
            },
        });
    });

    test("deletes data by id", async () => {
        const { result, waitForNextUpdate } = renderHook(() =>
            useEndpoint<null>(sampleApi.deleteSampleItem(123))
        );
        act(() => result.current.call());
        await waitForNextUpdate();

        expect(result.current.response.loading).toBeFalsy();
        expect(result.current.response.status).toBe(200);
        expect(result.current.response.message).toBe("");
        expect(result.current.response.data).toBeNull();
    });
});
