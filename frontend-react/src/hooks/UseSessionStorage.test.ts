import { act, renderHook } from "@testing-library/react-hooks";

import {
    getStoredOrg,
    getStoredSenderName,
} from "../contexts/SessionStorageTools";

import useSessionStorage, { StoreController } from "./UseSessionStorage";

describe("useSessionStorage", () => {
    test("default values", () => {
        const { result } = renderHook(() => useSessionStorage());

        expect(result.current.values.org).toBe(undefined);
        expect(result.current.values.senderName).toBe(undefined);
    });

    test("updates values in state and sessionStorage", async () => {
        const { result } = renderHook<null, StoreController>(() => {
            return useSessionStorage();
        });
        act(() => {
            result.current.updateSessionStorage({
                org: "testOrg2",
                senderName: "testSender2",
            });
        });

        expect(result.current.values.org).toBe("testOrg2");
        expect(result.current.values.senderName).toBe("testSender2");
        expect(getStoredOrg()).toBe("testOrg2");
        expect(getStoredSenderName()).toBe("testSender2");
    });
});
