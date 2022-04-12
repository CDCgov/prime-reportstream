import { act, renderHook } from "@testing-library/react-hooks";

import useDateRange from "./UseDateRange";

describe("UseDateRange", () => {
    test("renders with friendly default values", () => {
        const { result } = renderHook(() => useDateRange());
        expect(result.current.startRange.toISOString()).toEqual(
            "2998-01-01T00:00:00.000Z"
        );
        expect(result.current.endRange.toISOString()).toEqual(
            "2000-01-01T00:00:00.000Z"
        );
    });

    test("renders with given range", () => {
        const init = {
            startRange: new Date("2099-01-01"),
            endRange: new Date("1970-04-07"),
        };
        const { result } = renderHook(() => useDateRange(init));
        expect(result.current.startRange.toISOString()).toEqual(
            "2099-01-01T00:00:00.000Z"
        );
        expect(result.current.endRange.toISOString()).toEqual(
            "1970-04-07T00:00:00.000Z"
        );
    });

    test("can set full range", () => {
        const { result } = renderHook(() => useDateRange());
        act(() =>
            result.current.setRange({
                date1: "2022-12-31",
                date2: "2022-01-01",
            })
        );
        expect(result.current.startRange.toISOString()).toEqual(
            "2022-12-31T00:00:00.000Z"
        );
        expect(result.current.endRange.toISOString()).toEqual(
            "2022-01-01T00:00:00.000Z"
        );
    });

    test("can set partial range (ASC)", () => {
        const { result } = renderHook(() => useDateRange());
        act(() =>
            result.current.setRange({ date1: "2022-12-31", sort: "ASC" })
        );
        expect(result.current.startRange.toISOString()).toEqual(
            "2998-01-01T00:00:00.000Z"
        );
        expect(result.current.endRange.toISOString()).toEqual(
            "2022-12-31T00:00:00.000Z"
        );
    });

    test("can set partial range (DESC)", () => {
        const { result } = renderHook(() => useDateRange());
        act(() =>
            result.current.setRange({ date1: "2022-12-31", sort: "DESC" })
        );
        expect(result.current.startRange.toISOString()).toEqual(
            "2022-12-31T00:00:00.000Z"
        );
        expect(result.current.endRange.toISOString()).toEqual(
            "2000-01-01T00:00:00.000Z"
        );
    });
});
