import { act, renderHook } from "@testing-library/react-hooks";

import { Sender } from "../network/api/OrgApi";
import { orgServer } from "../__mocks__/OrganizationMockServer";

import useSenderMode, { SenderStatus } from "./UseSenderMode";

export const dummySender: Sender = {
    name: "testSender",
    organizationName: "testOrg",
    format: "CSV",
    topic: "covid-19",
    customerStatus: "testing", // Narrow this down to it's possible values
    schemaName: "test/covid-19-test",
};

/* This gets mocked for some component unit tests, so this line will
 * ensure it's not mocked for its own unit tests. */
jest.unmock("./UseSenderMode");

describe("useSenderMode", () => {
    beforeAll(() => orgServer.listen());
    afterEach(() => orgServer.resetHandlers());
    afterAll(() => orgServer.close());

    test("default value", () => {
        const { result } = renderHook<undefined, SenderStatus>(() => {
            return useSenderMode();
        });
        expect(result.current.status).toEqual("inactive");
    });

    test("update() gets values from server", async () => {
        const { result, waitForNextUpdate } = renderHook<
            undefined,
            SenderStatus
        >(() => {
            return useSenderMode();
        });
        act(() => {
            result.current?.update("testOrg", "testSender");
        });
        await waitForNextUpdate();

        expect(result.current.status).toEqual(dummySender.customerStatus);
    });
});
