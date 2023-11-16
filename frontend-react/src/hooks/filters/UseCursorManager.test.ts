import { renderHook, waitFor } from "../../utils/Test/render";

import useCursorManager, { CursorActionType } from "./UseCursorManager";

describe("Cursors", () => {
    test("Hook renders with default values", () => {
        const { result } = renderHook(() => useCursorManager());

        expect(result.current.cursors).toEqual({
            current: "",
            next: "",
            history: [],
        });
        expect(result.current.hasPrev).toBe(false);
        expect(result.current.hasNext).toBe(false);
    });

    test("Hook renders with parameter value", () => {
        const { result } = renderHook(() => useCursorManager("one"));

        expect(result.current.cursors).toEqual({
            current: "one",
            next: "",
            history: [],
        });
    });

    test("dispatch successfully adds next cursor", async () => {
        const { result } = renderHook(() => useCursorManager("one"));
        await waitFor(() => {
            result.current.update({
                type: CursorActionType.ADD_NEXT,
                payload: "two",
            });
        });
        expect(result.current.cursors).toEqual({
            current: "one",
            next: "two",
            history: [],
        });
    });

    test("dispatch successfully pages up", async () => {
        const { result } = renderHook(() => useCursorManager("one"));
        await waitFor(() => {
            result.current.update({
                type: CursorActionType.ADD_NEXT,
                payload: "two",
            });
            result.current.update({ type: CursorActionType.PAGE_UP });
            result.current.update({
                type: CursorActionType.ADD_NEXT,
                payload: "three",
            });
            result.current.update({ type: CursorActionType.PAGE_UP });
        });
        expect(result.current.cursors).toEqual({
            current: "three",
            next: "",
            history: ["one", "two"],
        });
    });

    test("dispatch successfully pages down", async () => {
        const { result } = renderHook(() => useCursorManager("one"));
        await waitFor(() => {
            result.current.update({
                type: CursorActionType.ADD_NEXT,
                payload: "two",
            });
            result.current.update({ type: CursorActionType.PAGE_UP });
            result.current.update({
                type: CursorActionType.ADD_NEXT,
                payload: "three",
            });
            result.current.update({ type: CursorActionType.PAGE_UP });
            result.current.update({ type: CursorActionType.PAGE_DOWN });
            result.current.update({ type: CursorActionType.PAGE_DOWN });
            result.current.update({
                type: CursorActionType.ADD_NEXT,
                payload: "two",
            });
        });
        expect(result.current.cursors).toEqual({
            current: "one",
            next: "two",
            history: [],
        });
    });

    test("dispatch successfully resets", async () => {
        const { result } = renderHook(() => useCursorManager("one"));
        await waitFor(() => {
            result.current.update({
                type: CursorActionType.ADD_NEXT,
                payload: "two",
            });
        });
        expect(result.current.cursors).toEqual({
            current: "one",
            next: "two",
            history: [],
        });
        await waitFor(() => {
            result.current.update({
                type: CursorActionType.RESET,
                payload: "one",
            });
        });
        expect(result.current.cursors).toEqual({
            current: "one",
            next: "",
            history: [],
        });
    });
});
