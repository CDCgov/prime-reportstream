import { renderHook, waitFor } from "../../utils/Test/render";

import usePages, { PageSettingsActionType } from "./UsePages";

describe("UsePages", () => {
    test("defaults to page number 1", () => {
        const { result } = renderHook(() => usePages());
        expect(result.current.settings.currentPage).toEqual(1);
    });
    test("defaults to page size of 10", () => {
        const { result } = renderHook(() => usePages());
        expect(result.current.settings.size).toEqual(10);
    });
    test("dispatch increments and decrements currentPage", async () => {
        const { result } = renderHook(() => usePages());
        await waitFor(() => {
            result.current.update({ type: PageSettingsActionType.INC_PAGE });
            result.current.update({ type: PageSettingsActionType.INC_PAGE });
        });
        expect(result.current.settings.currentPage).toEqual(3);
        await waitFor(() =>
            result.current.update({ type: PageSettingsActionType.DEC_PAGE }),
        );
        expect(result.current.settings.currentPage).toEqual(2);
        await waitFor(() =>
            result.current.update({ type: PageSettingsActionType.RESET }),
        );
        expect(result.current.settings.currentPage).toEqual(1);
    });
    test("dispatch updates size", async () => {
        const { result } = renderHook(() => usePages());
        await waitFor(() =>
            result.current.update({
                type: PageSettingsActionType.SET_SIZE,
                payload: {
                    size: 25,
                },
            }),
        );
        expect(result.current.settings.size).toEqual(25);
    });
});
