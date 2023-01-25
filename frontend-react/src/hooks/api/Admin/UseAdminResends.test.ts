import { setupServer } from "msw/node";

import { adminEndpoints } from "../../../config/api/admin";
import { mockAdminAction } from "../../../config/api/__mocks__/AdminData";
import { adminEndpointGroup } from "../../../config/api/__mocks__/AdminMockServer";
import { testQueryHook } from "../__mocks__/TestHook";

import { useAdminResends } from "./UseAdminResends";

const mockServer = setupServer(
    ...(adminEndpointGroup.endpoints.get(adminEndpoints.resend) ?? [])
);

describe(useAdminResends.name, () => {
    beforeAll(() => {
        mockServer.listen();
    });
    afterEach(() => mockServer.resetHandlers());
    afterAll(() => mockServer.close());

    testQueryHook(useAdminResends, [
        {
            args: [1],
            auth: true,
            data: [mockAdminAction],
        },
    ]);
});
