import { setupServer } from "msw/node";

import { adminEndpoints } from "../../../config/api/admin";
import { mockReceiverConnectionStatuses } from "../../../config/api/__mocks__/AdminData";
import { adminEndpointGroup } from "../../../config/api/__mocks__/AdminMockServer";
import { testQueryHook } from "../__mocks__/TestHook";

import { useAdminReceiversConnectionStatus } from "./UseAdminReceiversConnectionStatus";

const mockServer = setupServer(
    ...(adminEndpointGroup.endpoints.get(
        adminEndpoints.listReceiversConnectionStatus
    ) ?? [])
);

describe(useAdminReceiversConnectionStatus.name, () => {
    beforeAll(() => {
        mockServer.listen();
    });
    afterEach(() => mockServer.resetHandlers());
    afterAll(() => mockServer.close());

    testQueryHook(useAdminReceiversConnectionStatus, [
        {
            args: [new Date()],
            auth: true,
            data: mockReceiverConnectionStatuses,
        },
    ]);
});
