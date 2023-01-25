import { adminEndpoints } from "../admin";
import { HTTPMethods } from "../RSEndpoint";

import {
    mockAdminAction,
    mockReceiverConnectionStatuses,
    mockSendFailure,
} from "./AdminData";
import { RSEndpointGroup } from "./RSEndpointGroup";

export const adminEndpointGroup = new RSEndpointGroup([
    {
        endpoint: adminEndpoints.listReceiversConnectionStatus,
        meta: {
            [HTTPMethods.GET]: {
                response: mockReceiverConnectionStatuses,
            },
        },
    },
    {
        endpoint: adminEndpoints.resend,
        meta: {
            [HTTPMethods.GET]: {
                response: [mockAdminAction],
            },
        },
    },
    {
        endpoint: adminEndpoints.sendFailures,
        meta: {
            [HTTPMethods.GET]: {
                response: [mockSendFailure],
            },
        },
    },
]);
