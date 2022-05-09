import { act, renderHook } from "@testing-library/react-hooks";

import useDateRange, { RangeSettingsActionType } from "./UseDateRange";

describe("UseDateRange", () => {
    test("renders with default values", () => {
        const { result } = renderHook(() => useDateRange());
        expect(result.current.settings).toEqual({
            start: "3000-01-01T00:00:00.000Z",
            end: "2000-01-01T00:00:00.000Z",
        });
    });

    test("dispatch can update start", () => {
        const { result } = renderHook(() => useDateRange());
        act(() =>
            result.current.update({
                type: RangeSettingsActionType.UPDATE_FROM,
                payload: {
                    from: new Date("2022-12-31").toISOString(),
                },
            })
        );
        expect(result.current.settings).toEqual({
            start: "2022-12-31T00:00:00.000Z",
            end: "2000-01-01T00:00:00.000Z",
        });
    });

    test("dispatch can update end", () => {
        const { result } = renderHook(() => useDateRange());
        act(() =>
            result.current.update({
                type: RangeSettingsActionType.UPDATE_TO,
                payload: {
                    to: new Date("2022-01-01").toISOString(),
                },
            })
        );
        expect(result.current.settings).toEqual({
            start: "3000-01-01T00:00:00.000Z",
            end: "2022-01-01T00:00:00.000Z",
        });
    });

    test("dispatch can reset range", () => {
        const { result } = renderHook(() => useDateRange());
        act(() =>
            result.current.update({
                type: RangeSettingsActionType.UPDATE_TO,
                payload: {
                    to: new Date("2022-01-01").toISOString(),
                },
            })
        );
        act(() =>
            result.current.update({
                type: RangeSettingsActionType.RESET,
            })
        );
        expect(result.current.settings).toEqual({
            start: "3000-01-01T00:00:00.000Z",
            end: "2000-01-01T00:00:00.000Z",
        });
    });

    test("reset backdoor for manual update", () => {
        const { result } = renderHook(() => useDateRange());
        act(() =>
            result.current.update({
                type: RangeSettingsActionType.RESET,
                payload: {
                    from: new Date("2022-12-31").toISOString(),
                    to: new Date("2022-01-01").toISOString(),
                },
            })
        );
        expect(result.current.settings).toEqual({
            start: "2022-12-31T00:00:00.000Z",
            end: "2022-01-01T00:00:00.000Z",
        });
    });
});
