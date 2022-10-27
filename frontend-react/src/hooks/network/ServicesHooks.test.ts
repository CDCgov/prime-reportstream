import { renderHook } from "@testing-library/react-hooks";
import { AccessToken } from "@okta/okta-auth-js";

import {
    MembershipSettings,
    MembershipState,
    MemberType,
} from "../UseOktaMemberships";
import { orgServer } from "../../__mocks__/OrganizationMockServer";

import { useSessionServices } from "./ServicesHooks";

describe("useSessionServicesEffect", () => {
    beforeAll(() => orgServer.listen());
    afterEach(() => orgServer.resetHandlers());
    afterAll(() => orgServer.close());
    test("fetches and returns list of services", async () => {
        const fakeMembership: MembershipState = {
            initialized: true,
            activeMembership: {
                parsedName: "testOrg",
                memberType: MemberType.SENDER,
            } as MembershipSettings,
        };
        const { result, waitForNextUpdate } = renderHook(() =>
            useSessionServices(fakeMembership, {
                accessToken: "abc123",
            } as AccessToken)
        );
        await waitForNextUpdate();
        expect(result.current).toEqual([
            {
                name: "testSender",
                organizationName: "testOrg",
                format: "CSV",
                topic: "covid-19",
                customerStatus: "testing",
                schemaName: "test/covid-19-test",
            },
        ]);
    });
    test("won't fetch when membership is not initialized", () => {
        const fakeMembership: MembershipState = {
            initialized: false,
            activeMembership: {
                parsedName: "testOrg",
                memberType: MemberType.SENDER,
            } as MembershipSettings,
        };
        const { result } = renderHook(() =>
            useSessionServices(fakeMembership, {
                accessToken: "abc123",
            } as AccessToken)
        );
        expect(result.current).toBeUndefined();
    });
    test("won't fetch when membership is undefined", () => {
        const fakeMembership: MembershipState = {
            initialized: true,
            activeMembership: {} as MembershipSettings,
        };
        const { result } = renderHook(() =>
            useSessionServices(fakeMembership, {
                accessToken: "abc123",
            } as AccessToken)
        );
        expect(result.current).toBeUndefined();
    });
    test("won't fetch when access token is undefined", () => {
        const fakeMembership: MembershipState = {
            initialized: true,
            activeMembership: {} as MembershipSettings,
        };
        const { result } = renderHook(() =>
            useSessionServices(fakeMembership, undefined)
        );
        expect(result.current).toBeUndefined();
    });
});
