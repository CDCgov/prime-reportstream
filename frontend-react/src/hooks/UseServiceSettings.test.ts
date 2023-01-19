import { act, renderHook } from "@testing-library/react-hooks";

import { RSService } from "../config/endpoints/settings";

import { ServiceActionType, useServiceSettings } from "./UseServiceSettings";

const fauxService: RSService = {
    customerStatus: "testing",
    name: "my-org",
    organizationName: "My Organization",
    topic: "full-elr",
};

describe("useServiceSettings", () => {
    test("renders with undefined defaults", () => {
        const { result } = renderHook(() => useServiceSettings());
        expect(result.current.state).toEqual({
            active: undefined,
            senders: [],
            receivers: [],
        });
    });
    describe("dispatch", () => {
        test("can update active", () => {
            const { result } = renderHook(() => useServiceSettings());
            act(() => {
                result.current.dispatch({
                    type: ServiceActionType.SET_ACTIVE,
                    payload: { active: "test" },
                });
            });
            expect(result.current.state.active).toEqual("test");
        });
        test("can update senders", () => {
            const { result } = renderHook(() => useServiceSettings());
            act(() => {
                result.current.dispatch({
                    type: ServiceActionType.STASH_SERVICES,
                    payload: { senders: [fauxService] },
                });
            });
            expect(result.current.state.senders).toEqual([fauxService]);
        });
        test("can update receivers", () => {
            const { result } = renderHook(() => useServiceSettings());
            act(() => {
                result.current.dispatch({
                    type: ServiceActionType.STASH_SERVICES,
                    payload: { receivers: [fauxService] },
                });
            });
            expect(result.current.state.receivers).toEqual([fauxService]);
        });
    });
});
