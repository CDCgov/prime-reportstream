import { renderHook } from "@testing-library/react-hooks";

import { Sender } from "../network/api/OrgApi";
import { orgServer } from "../__mocks__/OrganizationMockServer";
import * as SessionContext from "../contexts/SessionContext";
import { parseOrgName } from "../utils/OrganizationUtils";

import useSenderMode from "./UseSenderMode";
import { StoreController } from "./UseSessionStorage";
import { MembershipController, MemberType } from "./UseGroups";

export const dummySender: Sender = {
    name: "testSender",
    organizationName: "testOrg",
    format: "CSV",
    topic: "covid-19",
    customerStatus: "testing",
    schemaName: "test/covid-19-test",
};

jest.mock("@okta/okta-react", () => ({
    useOktaAuth: () => {
        const authState = {
            isAuthenticated: true,
        };
        return { authState: authState };
    },
}));

const mockContext = jest.spyOn(SessionContext, "useSessionContext");

describe("useSenderMode", () => {
    beforeAll(() => orgServer.listen());
    afterEach(() => orgServer.resetHandlers());
    afterAll(() => orgServer.close());

    test("provides accurate sender mode", async () => {
        mockContext.mockReturnValue({
            memberships: {
                state: {
                    active: {
                        memberType: MemberType.SENDER,
                        parsedName: parseOrgName("DHSender_all-in-one-health"),
                    },
                },
            } as MembershipController,
            store: {} as StoreController,
        });
        const { result, waitForNextUpdate } = renderHook(() =>
            useSenderMode("testOrg", "testSender")
        );
        await waitForNextUpdate();
        expect(result.current).toEqual(dummySender.customerStatus);
    });
});
