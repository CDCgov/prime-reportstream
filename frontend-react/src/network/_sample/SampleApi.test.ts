import { sampleApi, SampleObject } from "./SampleApi";

describe("Sample API", () => {
    test("getSampleList", () => {
        const endpoint = sampleApi.getSampleList();
        expect(endpoint).toEqual({
            method: "GET",
            url: "http://testhost:9999/api/sample",
            headers: {
                Authorization: "Bearer [token]",
            },
            responseType: "json",
        });
    });

    test("postSampleItem", () => {
        const obj = new SampleObject("string", true, 1);
        const endpoint = sampleApi.postSampleItem(obj);
        expect(endpoint).toEqual({
            method: "POST",
            url: "http://testhost:9999/api/sample",
            headers: {
                Authorization: "Bearer [token]",
            },
            responseType: "json",
            data: obj,
        });
    });

    test("patchSampleItem", () => {
        const update = {
            bool: false,
        };
        const endpoint = sampleApi.patchSampleItem(123, update);
        expect(endpoint).toEqual({
            method: "PATCH",
            url: "http://testhost:9999/api/sample/123",
            headers: {
                Authorization: "Bearer [token]",
            },
            responseType: "json",
            data: {
                bool: false,
            },
        });
    });

    test("deleteSampleItem", () => {
        const endpoint = sampleApi.deleteSampleItem(123);
        expect(endpoint).toEqual({
            method: "DELETE",
            url: "http://testhost:9999/api/sample/123",
            headers: {
                Authorization: "Bearer [token]",
            },
            responseType: "json",
        });
    });
});
