import { renderHook, waitFor } from "../../utils/Test/render";

import useSortOrder, { SortSettingsActionType } from "./UseSortOrder";

describe("UseSortOrder", () => {
    test("renders with default values", async () => {
        const { result } = renderHook(() => useSortOrder());
        expect(result.current.settings).toEqual({
            column: "",
            order: "DESC",
            locally: false,
            localOrder: "DESC",
        });
    });

    test("dispatch changes column", async () => {
        const { result } = renderHook(() => useSortOrder());
        await waitFor(() =>
            result.current.update({
                type: SortSettingsActionType.CHANGE_COL,
                payload: {
                    column: "test",
                },
            }),
        );
        expect(result.current.settings).toEqual({
            column: "test",
            order: "DESC",
            locally: false,
            localOrder: "DESC",
        });
    });

    test("dispatch swaps order (non-locally)", async () => {
        const { result } = renderHook(() => useSortOrder());
        await waitFor(() =>
            result.current.update({
                type: SortSettingsActionType.SWAP_ORDER,
            }),
        );
        expect(result.current.settings).toEqual({
            column: "",
            order: "ASC",
            locally: false,
            localOrder: "DESC",
        });
    });

    test("dispatch sets and unsets locally sort", async () => {
        const { result } = renderHook(() => useSortOrder());
        await waitFor(() =>
            result.current.update({
                type: SortSettingsActionType.APPLY_LOCAL_SORT,
                payload: {
                    locally: true,
                },
            }),
        );
        expect(result.current.settings).toEqual({
            column: "",
            order: "DESC",
            locally: true,
            localOrder: "DESC",
        });
    });

    test("dispatch swaps order (non-locally)", async () => {
        const { result } = renderHook(() => useSortOrder());
        await waitFor(() =>
            result.current.update({
                type: SortSettingsActionType.SWAP_LOCAL_ORDER,
            }),
        );
        expect(result.current.settings).toEqual({
            column: "",
            order: "DESC",
            locally: false,
            localOrder: "ASC",
        });
    });

    test("dispatch resets settings", async () => {
        const { result } = renderHook(() => useSortOrder());
        await waitFor(() =>
            result.current.update({
                type: SortSettingsActionType.CHANGE_COL,
                payload: {
                    column: "test",
                },
            }),
        );
        await waitFor(() =>
            result.current.update({
                type: SortSettingsActionType.SWAP_ORDER,
            }),
        );
        await waitFor(() =>
            result.current.update({
                type: SortSettingsActionType.APPLY_LOCAL_SORT,
                payload: {
                    locally: true,
                },
            }),
        );
        expect(result.current.settings).toEqual({
            column: "test",
            order: "ASC",
            locally: true,
            localOrder: "DESC",
        });
        await waitFor(() =>
            result.current.update({
                type: SortSettingsActionType.RESET,
            }),
        );
        expect(result.current.settings).toEqual({
            column: "",
            order: "DESC",
            locally: false,
            localOrder: "DESC",
        });
    });

    test("dispatch has workaround for manual settings change", async () => {
        const { result } = renderHook(() => useSortOrder());
        await waitFor(() =>
            result.current.update({
                type: SortSettingsActionType.RESET,
                payload: {
                    column: "test",
                    order: "ASC",
                    locally: true,
                    localOrder: "ASC",
                },
            }),
        );
        expect(result.current.settings).toEqual({
            column: "test",
            order: "ASC",
            locally: true,
            localOrder: "ASC",
        });
    });
});
