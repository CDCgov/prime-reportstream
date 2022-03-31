import { act, renderHook } from "@testing-library/react-hooks";

import useCursorManager from "./UseCursorManager";

describe("useCursorManager", () => {
    test("Hook renders with default values", () => {
        const { result } = renderHook(() => useCursorManager());

        expect(result.current.values.cursors).toEqual(
            new Map<number, string>([[0, ""]])
        );
        expect(result.current.values.cursor).toBe("");
        expect(result.current.values.currentIndex).toBe(0);
        expect(result.current.values.hasPrev).toBe(false);
        expect(result.current.values.hasNext).toBe(false);
    });

    test("Hook renders with parameter value", () => {
        const { result } = renderHook(() => useCursorManager("one"));

        expect(result.current.values.cursors).toEqual(
            new Map<number, string>([[0, "one"]])
        );
        expect(result.current.values.cursor).toBe("one");
    });

    test("addNextCursor successfully appends map with cursor", () => {
        const { result } = renderHook(() => useCursorManager());

        act(() => {
            result.current.controller.addNextCursor("test");
        });

        expect(result.current.values.cursors).toEqual(
            new Map([
                [0, ""],
                [1, "test"],
            ])
        );
    });

    test("Simulate receiving new cursors w/ duplicates", () => {
        const { result } = renderHook(() => useCursorManager());
        const response = ["123", "456", "789"];
        const responseTwo = ["456", "789", "000"];

        /* After first response, add cursors */
        act(() => {
            response.forEach((c: string) => {
                result.current.controller.addNextCursor(c);
            });
        });

        expect(result.current.values.cursors).toEqual(
            new Map<number, string>([
                [0, ""],
                [1, "123"],
                [2, "456"],
                [3, "789"],
            ])
        );
        expect(result.current.values.hasNext).toBe(true);
        expect(result.current.values.hasPrev).toBe(false);

        /* Paging to the next page would mean you'd get two duplicate cursors and
         * one NEW cursor in this array; this should only add the new cursor. */
        act(() => {
            responseTwo.forEach((c: string) => {
                result.current.controller.addNextCursor(c);
            });
        });

        expect(result.current.values.cursors).toEqual(
            new Map<number, string>([
                [0, ""],
                [1, "123"],
                [2, "456"],
                [3, "789"],
                [4, "000"],
            ])
        );
        expect(result.current.values.hasNext).toBe(true);
        expect(result.current.values.hasPrev).toBe(false);
    });

    test("Simulate selecting a new cursor", () => {
        const { result } = renderHook(() => useCursorManager());
        act(() => result.current.controller.addNextCursor("next cursor"));
        act(() => result.current.controller.goTo(1));
        expect(result.current.values.cursor).toBe("next cursor");
    });

    test("Simulate selecting an invalid cursor", () => {
        const { result } = renderHook(() => useCursorManager());
        act(() => result.current.controller.addNextCursor("next cursor"));
        act(() => result.current.controller.goTo(33));
        expect(result.current.values.cursor).toBe("");
        expect(result.current.values.currentIndex).toBe(0);
    });
});
