import { setupServer } from "msw/node";

import { adminEndpoints } from "../../../config/api/admin";
import { mockSendFailure } from "../../../config/api/__mocks__/AdminData";
import { adminEndpointGroup } from "../../../config/api/__mocks__/AdminMockServer";
import { testQueryHook } from "../__mocks__/TestHook";

import { useAdminSendFailures } from "./UseAdminSendFailures";

const mockServer = setupServer(
    ...(adminEndpointGroup.endpoints.get(adminEndpoints.sendFailures) ?? [])
);

describe(useAdminSendFailures.name, () => {
    beforeAll(() => {
        mockServer.listen();
    });
    afterEach(() => mockServer.resetHandlers());
    afterAll(() => mockServer.close());

    testQueryHook(useAdminSendFailures, [
        {
            args: [1],
            auth: true,
            data: [mockSendFailure],
        },
    ]);
});
