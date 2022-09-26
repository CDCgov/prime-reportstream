import { wrapUseQuery } from "./AuthorizedFetchContext";

const mockUseQuery = jest.fn();

jest.mock("@tanstack/react-query", () => ({
    ...jest.requireActual("@tanstack/react-query"),
    useQuery: (key: unknown, fn: unknown, opts: unknown) =>
        mockUseQuery(key, fn, opts),
}));

describe("wrapUseQuery", () => {
    test("returns a fn that calls useQuery with `enabled` key correctly overloaded by passed initialized arg", () => {
        const initializedUseQuery = wrapUseQuery(true);
        const uninitializedUseQuery = wrapUseQuery(false);

        uninitializedUseQuery([], () => {});
        initializedUseQuery([], () => {});
        initializedUseQuery([], () => {}, { enabled: false });

        expect(mockUseQuery).toHaveBeenCalledTimes(3);

        const [firstCall, secondCall, thirdCall] = mockUseQuery.mock.calls;

        // wrap called with initialized === false
        expect(firstCall[2]).toEqual({ enabled: false });

        // wrap called with initialized === true
        expect(secondCall[2]).toEqual({ enabled: true });

        // wrap called with initialized === true, but overridden by options on useQuery call
        expect(thirdCall[2]).toEqual({ enabled: false });
    });
});
