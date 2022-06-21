import { renderHook } from "@testing-library/react-hooks";

import { mockSessionContext } from "../../contexts/__mocks__/SessionContext";
import {
    MembershipController,
    MembershipSettings,
} from "../../hooks/UseOktaMemberships";
import { SessionController } from "../../hooks/UseSessionStorage";
import { orgServer, testSender } from "../../__mocks__/OrganizationMockServer";

import { orgApi, useGetSenderDetail } from "./OrgApi";

const mockSession = {
    oktaToken: {
        accessToken: "TOKEN",
    },
    memberships: {
        state: {
            active: {
                parsedName: "ORGANIZATION",
            } as MembershipSettings,
        },
    } as MembershipController,
    store: {} as SessionController,
};

describe("Organization API", () => {
    beforeAll(() => orgServer.listen());
    afterEach(() => orgServer.resetHandlers());
    afterAll(() => orgServer.close());
    test("getOrgList", () => {
        const endpoint = orgApi.getOrgList();
        expect(endpoint).toEqual({
            method: "GET",
            url: `${process.env.REACT_APP_BACKEND_URL}/api/settings/organizations`,
            headers: {},
            responseType: "json",
        });
    });

    test("getOrgDetail", () => {
        const endpoint = orgApi.getOrgDetail("test");
        expect(endpoint).toEqual({
            method: "GET",
            url: `${process.env.REACT_APP_BACKEND_URL}/api/settings/organizations/test`,
            headers: {},
            responseType: "json",
        });
    });

    test("getSenderDetail", async () => {
        mockSessionContext.mockReturnValue(mockSession);
        const { result, waitForNextUpdate } = renderHook(() =>
            useGetSenderDetail("testOrg", "testSender")
        );
        await waitForNextUpdate();
        expect(result.current.data).toEqual(testSender);
    });
});
