import { renderHook } from "@testing-library/react-hooks";
import * as ReactRouter from "react-router";

import { useValidParams } from "./UseValidParams";

const mockUseParams = jest.spyOn(ReactRouter, "useParams");
describe("useValidParams", () => {
    test("throws on invalid params", () => {
        mockUseParams.mockImplementation(() => ({
            two: "test",
        }));
        const { result } = renderHook(() => useValidParams(["one", "two"]));
        expect(result.error?.message).toEqual(
            "Expected param at key {one} was undefined"
        );
    });
    test("returns params when valid", () => {
        mockUseParams.mockImplementation(() => ({
            one: "one",
            two: "two",
            three: "three",
        }));
        const { result } = renderHook(() => useValidParams(["one", "two"]));
        expect(result.current).toEqual({
            one: "one",
            two: "two",
            three: "three",
        });
    });
});
