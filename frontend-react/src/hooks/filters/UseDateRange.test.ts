import { act } from "@testing-library/react";

import { renderHook } from "../../utils/CustomRenderUtils";

import useDateRange, {
    getEndOfDay,
    RangeSettingsActionType,
} from "./UseDateRange";

describe("UseDateRange", () => {
    test("renders with default values", () => {
        const { result } = renderHook(() => useDateRange());
        expect(result.current.settings).toEqual({
            to: "3000-01-01T00:00:00.000Z",
            from: "2000-01-01T00:00:00.000Z",
        });
    });

    test("dispatch can update From", () => {
        const { result } = renderHook(() => useDateRange());
        act(() =>
            result.current.update({
                type: RangeSettingsActionType.UPDATE_FROM,
                payload: {
                    from: new Date("2022-12-31").toISOString(),
                },
            }),
        );
        expect(result.current.settings).toEqual({
            to: "3000-01-01T00:00:00.000Z",
            from: "2022-12-31T00:00:00.000Z",
        });
    });

    test("dispatch can update To", () => {
        const { result } = renderHook(() => useDateRange());
        act(() =>
            result.current.update({
                type: RangeSettingsActionType.UPDATE_TO,
                payload: {
                    to: new Date("2022-01-01").toISOString(),
                },
            }),
        );
        expect(result.current.settings).toEqual({
            to: "2022-01-01T00:00:00.000Z",
            from: "2000-01-01T00:00:00.000Z",
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
            }),
        );
        act(() =>
            result.current.update({
                type: RangeSettingsActionType.RESET,
            }),
        );
        expect(result.current.settings).toEqual({
            to: "3000-01-01T00:00:00.000Z",
            from: "2000-01-01T00:00:00.000Z",
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
            }),
        );
        expect(result.current.settings).toEqual({
            from: "2022-12-31T00:00:00.000Z",
            to: "2022-01-01T00:00:00.000Z",
        });
    });
});

describe("getEndOfDay", () => {
    const date = new Date("2023-02-17T00:00:00.000Z");

    test("returns the end of the day in UTC", () => {
        expect(getEndOfDay(date)).toEqual(new Date("2023-02-17T23:59:59.999Z"));
    });

    test("does not modify the original Date instance", () => {
        getEndOfDay(date);
        expect(date.toISOString()).toEqual("2023-02-17T00:00:00.000Z");
    });
});
