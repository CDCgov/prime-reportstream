import { act, renderHook } from "@testing-library/react-hooks";

import useSortOrder, { SortActionType } from "./UseSortOrder";

describe("UseSortOrder", () => {
    test("renders with default values", () => {
        const { result } = renderHook(() => useSortOrder());
        expect(result.current.settings).toEqual({
            column: "",
            order: "DESC",
        });
    });

    test("dispatch changes column", () => {
        const { result } = renderHook(() => useSortOrder());
        act(() =>
            result.current.update({
                type: SortActionType.CHANGE_COL,
                payload: {
                    column: "test",
                },
            })
        );
        expect(result.current.settings).toEqual({
            column: "test",
            order: "DESC",
        });
    });

    test("dispatch swaps order", () => {
        const { result } = renderHook(() => useSortOrder());
        act(() =>
            result.current.update({
                type: SortActionType.SWAP_ORDER,
            })
        );
        expect(result.current.settings).toEqual({
            column: "",
            order: "ASC",
        });
    });

    test("dispatch resets settings", () => {
        const { result } = renderHook(() => useSortOrder());
        act(() =>
            result.current.update({
                type: SortActionType.CHANGE_COL,
                payload: {
                    column: "test",
                },
            })
        );
        act(() =>
            result.current.update({
                type: SortActionType.SWAP_ORDER,
            })
        );
        expect(result.current.settings).toEqual({
            column: "test",
            order: "ASC",
        });
        act(() =>
            result.current.update({
                type: SortActionType.RESET,
            })
        );
        expect(result.current.settings).toEqual({
            column: "",
            order: "DESC",
        });
    });

    test("dispatch has workaround for manual settings change", () => {
        const { result } = renderHook(() => useSortOrder());
        act(() =>
            result.current.update({
                type: SortActionType.RESET,
                payload: {
                    column: "test",
                    order: "ASC",
                },
            })
        );
        expect(result.current.settings).toEqual({
            column: "test",
            order: "ASC",
        });
    });
});
