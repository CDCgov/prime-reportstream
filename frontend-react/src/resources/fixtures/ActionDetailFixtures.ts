import { Fixture } from "@rest-hooks/test";

import ActionDetailsResource from "../ActionDetailsResource";

export const success: Fixture[] = [
    {
        endpoint: ActionDetailsResource.detail(),
        args: [
            {
                actionId: 12345,
                organization: "Jest",
            },
        ],
        response: ActionDetailsResource.dummy(),
        error: false,
    },
];

export const failure: Fixture[] = [
    {
        endpoint: ActionDetailsResource.detail(),
        args: [{}],
        response: [],
        error: true,
    },
];
