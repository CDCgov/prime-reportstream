import { renderHook } from "@testing-library/react-hooks";

import { sampleServer } from "../../__mocks__/SampleServer";
import { sampleApi, SampleObject } from "../_sample/SampleApi";

import { useEndpoint } from "./UseEndpoint";

const obj = new SampleObject("string", true, 123);

/* Tests DO pass, but the test renderer isn't fond of something about
 * the way the hook renders. Gives the ol' "can't perform state update
 * on unmounted component" console error. */

describe("UseEndpoint.ts", () => {
    /* Handles setup, refresh, and closing of mock service worker */
    beforeAll(() => sampleServer.listen());
    afterEach(() => sampleServer.resetHandlers());
    afterAll(() => sampleServer.close());

    test("returns data from get call", async () => {
        const { result, waitForNextUpdate } = renderHook(() =>
            useEndpoint<SampleObject[]>(sampleApi.getSampleList())
        );
        await waitForNextUpdate();

        expect(result.current.loading).toBeFalsy();
        expect(result.current.status).toBe(200);
        expect(result.current.message).toBe("");
        expect(result.current.data).toEqual([obj, obj]);
    });

    test("posts data from post call", async () => {
        const { result, waitForNextUpdate } = renderHook(() =>
            useEndpoint<SampleObject>(sampleApi.postSampleItem(obj))
        );
        await waitForNextUpdate();

        expect(result.current.loading).toBeFalsy();
        expect(result.current.status).toBe(202);
        expect(result.current.message).toBe("");
        expect(result.current.data).toEqual(obj);
    });

    test("patches data with new values", async () => {
        const { result, waitForNextUpdate } = renderHook(() =>
            useEndpoint<Partial<SampleObject>>(
                sampleApi.patchSampleItem(123, {
                    bool: false,
                })
            )
        );
        await waitForNextUpdate();

        expect(result.current.loading).toBeFalsy();
        expect(result.current.status).toBe(202);
        expect(result.current.message).toBe("");
        expect(result.current.data).toEqual({
            received: {
                bool: false,
            },
        });
    });

    test("deletes data by id", async () => {
        const { result, waitForNextUpdate } = renderHook(() =>
            useEndpoint<null>(sampleApi.deleteSampleItem(123))
        );
        await waitForNextUpdate();

        expect(result.current.loading).toBeFalsy();
        expect(result.current.status).toBe(200);
        expect(result.current.message).toBe("");
        expect(result.current.data).toBeNull();
    });
});
