import { renderHook } from "@testing-library/react-hooks";

import { Sender } from "../network/api/OrgApi";
import { orgServer } from "../__mocks__/OrganizationMockServer";

import useSenderMode from "./UseSenderMode";
import { waitFor } from "@testing-library/react";

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

describe("useSenderMode", () => {
    beforeAll(() => orgServer.listen());
    afterEach(() => orgServer.resetHandlers());
    afterAll(() => orgServer.close());

    test("provides accurate sender mode", async () => {
        const { result, waitForNextUpdate } = renderHook(() =>
            useSenderMode("testOrg", "testSender")
        );
        await waitFor(() =>
            expect(result.current).toEqual(dummySender.customerStatus)
        );
    });
});
