import { act, renderHook } from "@testing-library/react-hooks";

import { settingsServer } from "../../__mocks__/SettingsMockServer";
import { MembershipState, MemberType } from "../UseOktaMemberships";
import { mockToken } from "../../utils/TestUtils";

import { cleanServicesArray, useMemberServices } from "./ServiceHooks";

const happyState: MembershipState = {
    activeMembership: {
        parsedName: "test-org",
        memberType: MemberType.RECEIVER,
    },
    initialized: true,
};
const happyToken = mockToken({
    claims: {
        sub: "",
        organization: ["DHtest-org"],
    },
});

describe("useMemberServices", () => {
    beforeAll(() => settingsServer.listen());
    afterEach(() => settingsServer.resetHandlers());
    afterAll(() => settingsServer.close());
    test("happily provides services", async () => {
        const { result, waitForNextUpdate } = renderHook(() =>
            useMemberServices(happyState, happyToken)
        );
        await waitForNextUpdate();
        expect(result.current.receivers).toHaveLength(2);
        expect(result.current.senders).toHaveLength(2);
        expect(result.current.activeService).toEqual("default");
    });

    test("does not fetch when state is not ready", () => {
        const sadState: MembershipState = {
            activeMembership: null,
            initialized: true,
        };
        const { result } = renderHook(() =>
            useMemberServices(sadState, happyToken)
        );
        expect(result.current.receivers).toBeUndefined();
        expect(result.current.senders).toBeUndefined();
        expect(result.current.activeService).toEqual("default");
    });

    test("does not fetch with undefined token", () => {
        const { result } = renderHook(() =>
            useMemberServices(happyState, undefined)
        );
        expect(result.current.receivers).toBeUndefined();
        expect(result.current.senders).toBeUndefined();
        expect(result.current.activeService).toEqual("default");
    });

    describe("controls activeService", () => {
        test("using controller", async () => {
            const { result, waitForNextUpdate } = renderHook(() =>
                useMemberServices(happyState, happyToken)
            );
            await waitForNextUpdate();
            expect(result.current.receivers).toHaveLength(2);
            expect(result.current.senders).toHaveLength(2);
            expect(result.current.activeService).toEqual("default");
            act(() => {
                result.current.setActiveService("new-service");
            });
            expect(result.current.activeService).toEqual("new-service");
        });
    });

    describe("helper functions", () => {
        test("cleanServiceArray", () => {
            const dirtyValue = {
                name: "my-setting-name",
                customerStatus: "active",
                organizationName: "my-organization",
                topic: "covid-19",
                dirt: "smudge",
                scratch: "dent",
                spoiled: "mold",
            };
            const cleanValue = cleanServicesArray([dirtyValue]);
            expect(cleanValue[0]).toEqual({
                name: "my-setting-name",
                customerStatus: "active",
                organizationName: "my-organization",
                topic: "covid-19",
            });
        });
    });
});
