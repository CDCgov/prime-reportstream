import { act, renderHook } from "@testing-library/react-hooks";

import useSortOrder, { SortSettingsActionType } from "./UseSortOrder";

describe("UseSortOrder", () => {
    test("renders with default values", () => {
        const { result } = renderHook(() => useSortOrder());
        expect(result.current.settings).toEqual({
            column: "",
            order: "DESC",
            locally: false,
        });
    });

    test("dispatch changes column", () => {
        const { result } = renderHook(() => useSortOrder());
        act(() =>
            result.current.update({
                type: SortSettingsActionType.CHANGE_COL,
                payload: {
                    column: "test",
                },
            })
        );
        expect(result.current.settings).toEqual({
            column: "test",
            order: "DESC",
            locally: false,
        });
    });

    test("dispatch swaps order", () => {
        const { result } = renderHook(() => useSortOrder());
        act(() =>
            result.current.update({
                type: SortSettingsActionType.SWAP_ORDER,
            })
        );
        expect(result.current.settings).toEqual({
            column: "",
            order: "ASC",
            locally: false,
        });
    });

    test("dispatch sets and unsets localSortFunction", () => {
        const { result } = renderHook(() => useSortOrder());
        act(() =>
            result.current.update({
                type: SortSettingsActionType.APPLY_LOCAL_SORT,
                payload: {
                    locally: true,
                },
            })
        );
        expect(result.current.settings).toEqual({
            column: "",
            order: "DESC",
            locally: true,
        });
    });

    test("dispatch resets settings", () => {
        const { result } = renderHook(() => useSortOrder());
        act(() =>
            result.current.update({
                type: SortSettingsActionType.CHANGE_COL,
                payload: {
                    column: "test",
                },
            })
        );
        act(() =>
            result.current.update({
                type: SortSettingsActionType.SWAP_ORDER,
            })
        );
        act(() =>
            result.current.update({
                type: SortSettingsActionType.APPLY_LOCAL_SORT,
                payload: {
                    locally: true,
                },
            })
        );
        expect(result.current.settings).toEqual({
            column: "test",
            order: "ASC",
            locally: true,
        });
        act(() =>
            result.current.update({
                type: SortSettingsActionType.RESET,
            })
        );
        expect(result.current.settings).toEqual({
            column: "",
            order: "DESC",
            locally: false,
        });
    });

    test("dispatch has workaround for manual settings change", () => {
        const { result } = renderHook(() => useSortOrder());
        act(() =>
            result.current.update({
                type: SortSettingsActionType.RESET,
                payload: {
                    column: "test",
                    order: "ASC",
                    locally: true,
                },
            })
        );
        expect(result.current.settings).toEqual({
            column: "test",
            order: "ASC",
            locally: true,
        });
    });
});
