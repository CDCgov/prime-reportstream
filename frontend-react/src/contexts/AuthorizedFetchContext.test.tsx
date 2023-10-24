import { wrapUseQuery } from "./AuthorizedFetchContext";

const mockUseQuery = jest.fn();

jest.mock("@tanstack/react-query", () => ({
    ...jest.requireActual("@tanstack/react-query"),
    useQuery: (key: unknown, fn: unknown, opts: unknown) =>
        mockUseQuery(key, fn, opts),
}));

describe("wrapUseQuery", () => {
    test("returns a fn that calls useQuery with `enabled` key correctly overloaded by passed initialized arg", () => {
        const initializedUseQuery = wrapUseQuery();

        initializedUseQuery([], () => {});
        initializedUseQuery([], () => {}, { enabled: false });

        expect(mockUseQuery).toHaveBeenCalledTimes(2);

        const [firstCall, secondCall] = mockUseQuery.mock.calls;

        // wrap called with enabled true by default
        expect(firstCall[2]).toEqual({ enabled: true });

        // wrap called with initialized === true, but overridden by options on useQuery call
        expect(secondCall[2]).toEqual({ enabled: false });
    });
});
